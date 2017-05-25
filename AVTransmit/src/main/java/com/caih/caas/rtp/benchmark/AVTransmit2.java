/*
 * @(#)com.caih.caas.rtp.benchmark.AVTransmit2.java	1.4 01/03/13
 *
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Sun grants you ("Licensee") a non-exclusive, royalty free, license to use,
 * modify and redistribute this software in source and binary code form,
 * provided that i) this copyright notice and license appear on all copies of
 * the software; and ii) Licensee does not utilize the software in a manner
 * which is disparaging to Sun.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE
 * LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING
 * OR DISTRIBUTING THE SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS
 * LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT,
 * INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF
 * OR INABILITY TO USE SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * This software is not designed or intended for use in on-line control of
 * aircraft, air traffic, aircraft navigation or aircraft communications; or in
 * the design, construction, operation or maintenance of any nuclear
 * facility. Licensee represents and warrants that it will not use or
 * redistribute the Software for such purposes.
 */

package com.caih.caas.rtp.benchmark;

import java.awt.*;
import java.io.*;
import java.net.InetAddress;
import javax.media.*;
import javax.media.control.BufferControl;
import javax.media.protocol.*;
import javax.media.protocol.DataSource;
import javax.media.format.*;
import javax.media.control.TrackControl;
import javax.media.control.QualityControl;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.*;

public class AVTransmit2 implements ReceiveStreamListener {

    // Input MediaLocator
    // Can be a file or http or capture source
    private MediaLocator locator;
    private SessionLabel bindSession;
    private SessionLabel destSession;

    private Processor processor = null;
    private RTPManager rtpMgrs[];
    private DataSource dataOutput = null;
    private PlayerWindow playerWindow;

    public AVTransmit2(MediaLocator locator,
                       SessionLabel bindSession,
                       SessionLabel destSession) {

        this.locator = locator;
        this.bindSession = bindSession;
        this.destSession = destSession;
    }

    /**
     * Starts the transmission. Returns null if transmission started ok.
     * Otherwise it returns a string with the reason why the setup failed.
     */
    public synchronized String start() {
        String result;

        // Create a processor for the specified media locator
        // and program it to output JPEG/RTP
        result = createProcessor();
        if (result != null)
            return result;

        // Create an RTP session to transmit the output of the
        // processor to the specified IP address and port no.
        result = createTransmitter();
        if (result != null) {
            processor.close();
            processor = null;
            return result;
        }

        // Start the transmission
        processor.start();

        return null;
    }

    /**
     * Stops the transmission if already started
     */
    public void stop() {
        synchronized (this) {
            if (processor != null) {
                processor.stop();
                processor.close();
                processor = null;

                setPlayerWindow(null);

                for (int i = 0; i < rtpMgrs.length; i++) {
                    rtpMgrs[i].removeTargets("Session ended.");
                    rtpMgrs[i].dispose();
                }
            }
        }
    }

    private String createProcessor() {
        if (locator == null)
            return "Locator is null";

        DataSource ds;
        DataSource clone;

        try {
            ds = javax.media.Manager.createDataSource(locator);
        } catch (Exception e) {
            return "Couldn't create DataSource";
        }

        // Try to create a processor to handle the input media locator
        try {
            processor = javax.media.Manager.createProcessor(ds);
        } catch (NoProcessorException npe) {
            return "Couldn't create processor";
        } catch (IOException ioe) {
            return "IOException creating processor";
        }

        // Wait for it to configure
        boolean result = waitForState(processor, Processor.Configured);
        if (result == false)
            return "Couldn't configure processor";

        // Get the tracks from the processor
        TrackControl[] tracks = processor.getTrackControls();

        // Do we have atleast one track?
        if (tracks == null || tracks.length < 1)
            return "Couldn't find tracks in processor";

        // Set the output content descriptor to RAW_RTP
        // This will limit the supported formats reported from
        // Track.getSupportedFormats to only valid RTP formats.
        ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
        processor.setContentDescriptor(cd);

        Format supported[];
        Format chosen;
        boolean atLeastOneTrack = false;

        // Program the tracks.
        for (int i = 0; i < tracks.length; i++) {
            Format format = tracks[i].getFormat();
            if (tracks[i].isEnabled()) {

                supported = tracks[i].getSupportedFormats();

                // We've set the output content to the RAW_RTP.
                // So all the supported formats should work with RTP.
                // We'll just pick the first one.

                if (supported.length > 0) {
                    if (supported[0] instanceof VideoFormat) {
                        // For video formats, we should double check the
                        // sizes since not all formats work in all sizes.
                        chosen = checkForVideoSizes(tracks[i].getFormat(),
                                supported[0]);
                    } else
                        chosen = supported[0];
                    tracks[i].setFormat(chosen);
                    System.err.println("Track " + i + " is set to transmit as:");
                    System.err.println("  " + chosen);
                    atLeastOneTrack = true;
                } else
                    tracks[i].setEnabled(false);
            } else
                tracks[i].setEnabled(false);
        }

        if (!atLeastOneTrack)
            return "Couldn't set any of the tracks to a valid RTP format";

        // Realize the processor. This will internally create a flow
        // graph and attempt to create an output datasource for JPEG/RTP
        // audio frames.
        result = waitForState(processor, Controller.Realized);
        if (result == false)
            return "Couldn't realize processor";

        // Set the JPEG quality to .5.
        setJPEGQuality(processor, 0.5f);

        // Get the output data source of the processor
        dataOutput = processor.getDataOutput();

        return null;
    }


    /**
     * Use the RTPManager API to create sessions for each media
     * track of the processor.
     */
    private String createTransmitter() {

        // Cheated.  Should have checked the type.
        PushBufferDataSource pbds = (PushBufferDataSource) dataOutput;
        PushBufferStream pbss[] = pbds.getStreams();

        rtpMgrs = new RTPManager[pbss.length];
        SessionAddress localAddr, destAddr;
        SendStream sendStream;
        int localPort;
        int destPort;

        for (int i = 0; i < pbss.length; i++) {
            try {
                rtpMgrs[i] = RTPManager.newInstance();
                rtpMgrs[i].addReceiveStreamListener(this);
                rtpMgrs[i].addRemoteListener(new RTPManagerRemoteEventListener());

                // The local session address will be created on the
                // same port as the the target port. This is necessary
                // if you use com.caih.caas.rtp.benchmark.AVTransmit2 in conjunction with JMStudio.
                // JMStudio assumes -  in a unicast session - that the
                // transmitter transmits from the same port it is receiving
                // on and sends RTCP Receiver Reports back to this port of
                // the transmitting host.

                localPort = bindSession.getPort() + 2 * i;
                destPort = destSession.getPort() + 2 * i;

                localAddr = new SessionAddress(InetAddress.getByName(bindSession.getIpAddr()), localPort);
                destAddr = new SessionAddress(InetAddress.getByName(destSession.getIpAddr()), destPort);

                SourceDescription[] srcDescList = JMFUtils.createSourceDescriptions(localAddr);
                JMFUtils.initializeRTPManager(rtpMgrs[i], localAddr, srcDescList);

                BufferControl buffCtrl = (BufferControl) rtpMgrs[i].getControl("javax.media.control.BufferControl");
                if (buffCtrl != null) {
                    buffCtrl.setBufferLength(350);
                    buffCtrl.setEnabledThreshold(true);
                    buffCtrl.setMinimumThreshold(100);
                    System.err.println("Setup Buffer Control: buffLen[350] minThreshold[100] in ms unit.");
                }

                rtpMgrs[i].addTarget(destAddr);

                System.err.println("Created RTP session: " + destSession.getIpAddr() + "/" + destPort);
                System.err.println("Session local addr: " + localAddr);
                System.err.println("Session remote addr: " + destAddr);

                sendStream = rtpMgrs[i].createSendStream(dataOutput, i);
                sendStream.start();
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        return null;
    }


    /**
     * For JPEG and H263, we know that they only work for particular
     * sizes.  So we'll perform extra checking here to make sure they
     * are of the right sizes.
     */
    Format checkForVideoSizes(Format original, Format supported) {

        int width, height;
        Dimension size = ((VideoFormat) original).getSize();
        Format jpegFmt = new Format(VideoFormat.JPEG_RTP);
        Format h263Fmt = new Format(VideoFormat.H263_RTP);

        if (supported.matches(jpegFmt)) {
            // For JPEG, make sure width and height are divisible by 8.
            width = (size.width % 8 == 0 ? size.width :
                    (int) (size.width / 8) * 8);
            height = (size.height % 8 == 0 ? size.height :
                    (int) (size.height / 8) * 8);
        } else if (supported.matches(h263Fmt)) {
            // For H.263, we only support some specific sizes.
            if (size.width < 128) {
                width = 128;
                height = 96;
            } else if (size.width < 176) {
                width = 176;
                height = 144;
            } else {
                width = 352;
                height = 288;
            }
        } else {
            // We don't know this particular format.  We'll just
            // leave it alone then.
            return supported;
        }

        return (new VideoFormat(null,
                new Dimension(width, height),
                Format.NOT_SPECIFIED,
                null,
                Format.NOT_SPECIFIED)).intersects(supported);
    }


    /**
     * Setting the encoding quality to the specified value on the JPEG encoder.
     * 0.5 is a good default.
     */
    void setJPEGQuality(Player p, float val) {

        Control cs[] = p.getControls();
        QualityControl qc = null;
        VideoFormat jpegFmt = new VideoFormat(VideoFormat.JPEG);

        // Loop through the controls to find the Quality control for
        // the JPEG encoder.
        for (int i = 0; i < cs.length; i++) {

            if (cs[i] instanceof QualityControl &&
                    cs[i] instanceof Owned) {
                Object owner = ((Owned) cs[i]).getOwner();

                // Check to see if the owner is a Codec.
                // Then check for the output format.
                if (owner instanceof Codec) {
                    Format fmts[] = ((Codec) owner).getSupportedOutputFormats(null);
                    for (int j = 0; j < fmts.length; j++) {
                        if (fmts[j].matches(jpegFmt)) {
                            qc = (QualityControl) cs[i];
                            qc.setQuality(val);
                            System.err.println("- Setting quality to " +
                                    val + " on " + qc);
                            break;
                        }
                    }
                }
                if (qc != null)
                    break;
            }
        }
    }


    /****************************************************************
     * Convenience methods to handle processor's state changes.
     ****************************************************************/

    private Integer stateLock = new Integer(0);
    private boolean failed = false;

    Integer getStateLock() {
        return stateLock;
    }

    void setFailed() {
        failed = true;
    }

    private synchronized boolean waitForState(Processor p, int state) {
        p.addControllerListener(new StateListener());
        failed = false;

        // Call the required method on the processor
        if (state == Processor.Configured) {
            p.configure();
        } else if (state == Processor.Realized) {
            p.realize();
        }

        // Wait until we get an event that confirms the
        // success of the method, or a failure event.
        // See StateListener inner class
        while (p.getState() < state && !failed) {
            synchronized (getStateLock()) {
                try {
                    getStateLock().wait();
                } catch (InterruptedException ie) {
                    return false;
                }
            }
        }

        if (failed)
            return false;
        else
            return true;
    }

    @Override
    public void update(ReceiveStreamEvent receiveStreamEvent) {
        Participant participant = receiveStreamEvent.getParticipant();    // could be null.
        ReceiveStream stream = receiveStreamEvent.getReceiveStream();  // could be null.

        if (receiveStreamEvent instanceof RemotePayloadChangeEvent) {
            System.err.println("  - Received an RTP PayloadChangeEvent.");
            System.err.println("Sorry, cannot handle payload change.");
            System.exit(0);
        } else if (receiveStreamEvent instanceof NewReceiveStreamEvent) {
            try {
                DataSource ds = stream.getDataSource();

                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl("javax.media.rtp.RTPControl");
                if (ctl != null) {
                    System.err.println("  - Recevied new RTP stream: " + ctl.getFormat());
                } else {
                    System.err.println("  - Recevied new RTP stream");
                }

                if (participant == null)
                    System.err.println("      The sender of this stream had yet to be identified.");
                else {
                    System.err.println("      The stream comes from: " + participant.getCNAME());
                }

                if (GlobalOptionHelper.isShowGUI()) {
                    // create a player by passing datasource to the Media Manager
                    Player p = createMediaPlayer(ds);
                    p.realize();
                    PlayerWindow pw = new PlayerWindow(p, stream);
                    setPlayerWindow(pw);
                } else {
                    ds.start();
                }
            } catch (Exception e) {
                System.err.println("NewReceiveStreamEvent exception " + e.getMessage());
                return;
            }
        } else if (receiveStreamEvent instanceof StreamMappedEvent) {
            if (stream != null && stream.getDataSource() != null) {
                DataSource ds = stream.getDataSource();
                // Find out the formats.
                RTPControl ctl = (RTPControl) ds.getControl("javax.media.rtp.RTPControl");
                System.err.println("  - The previously unidentified stream ");
                if (ctl != null) {
                    System.err.println("      " + ctl.getFormat());
                }
                System.err.println("      had now been identified as sent by: " + participant.getCNAME());
            }
        } else if (receiveStreamEvent instanceof ByeEvent) {
            System.err.println("  - Got \"bye\" from: " + participant.getCNAME());
            stop();
        } else if (receiveStreamEvent instanceof TimeoutEvent) {
            System.err.println("  - Receive timeout from: " + participant.getCNAME());
            stop();
        }
    }

    private Player createMediaPlayer(DataSource ds) throws IOException, NoPlayerException {
        Player p = javax.media.Manager.createPlayer(ds);
        if (p == null) {
            return null;
        }

        p.addControllerListener(new ControllerAdapter() {
            PlayerWindow find(Player player) {
                if (playerWindow != null && playerWindow.player == player) {
                    return playerWindow;
                } else {
                    return null;
                }
            }

            @Override
            public void realizeComplete(RealizeCompleteEvent e) {
                Player player = (Player) e.getSourceController();
                PlayerWindow pw = find(player);
                if (pw == null) {
                    // Some strange happened.
                    System.err.println("Internal error!");
                    System.exit(-1);
                }

                pw.initialize();
                pw.setVisible(true);
                player.start();
            }

            @Override
            public void controllerError(ControllerErrorEvent e) {
                Player player = (Player) e.getSourceController();
                player.removeControllerListener(this);
                PlayerWindow pw = find(player);
                if (pw != null) {
                    setPlayerWindow(null);
                }
                System.err.println("AVReceive2 internal error: " + e);
            }
        });
        return p;
    }

    protected void setPlayerWindow(PlayerWindow playerWindow) {
        if (this.playerWindow != playerWindow) {
            if (this.playerWindow != null) {
                this.playerWindow.close();
                this.playerWindow = null;
            }

            this.playerWindow = playerWindow;
        }
    }

    /****************************************************************
     * Inner Classes
     ****************************************************************/

    class StateListener implements ControllerListener {

        public void controllerUpdate(ControllerEvent ce) {

            // If there was an error during configure or
            // realize, the processor will be closed
            if (ce instanceof ControllerClosedEvent)
                setFailed();

            // All controller events, send a notification
            // to the waiting thread in waitForState method.
            if (ce instanceof ControllerEvent) {
                synchronized (getStateLock()) {
                    getStateLock().notifyAll();
                }
            }
        }
    }
}

