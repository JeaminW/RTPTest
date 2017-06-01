package com.caih.caas.rtp.benchmark;

import javax.media.Format;
import javax.media.Manager;
import javax.media.NoProcessorException;
import javax.media.Processor;
import javax.media.control.BufferControl;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import javax.media.rtp.rtcp.SourceDescription;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jeaminw on 17/5/5.
 */
public class RTPSwitch implements ReceiveStreamListener {
    SessionLabel[] bindSessions;
    SessionLabel[] destSessions;
    List<RTPManager> rtpMngrs;
    List<Processor> transcodingProcList;

    Map<RTPManager, ReceiveStream> receivedStreamsMap = new ConcurrentHashMap<>(2);
    final AtomicInteger receivedStreamCount = new AtomicInteger(0);

    public RTPSwitch(SessionLabel[] bindSessions, SessionLabel[] destSessions) {
        this.bindSessions = bindSessions;
        this.destSessions = destSessions;
    }

    public boolean init() {
        if (bindSessions == null || bindSessions.length != 2) {
            throw new IllegalArgumentException("Needs 2 binded sessions: " + bindSessions);
        }

        if (destSessions == null || destSessions.length != 2) {
            throw new IllegalArgumentException("Needs 2 dest sessions: " + destSessions);
        }

        try {
            InetAddress destIpAddr;
            SessionAddress localAddr;
            SessionAddress destAddr;
            rtpMngrs = new ArrayList<>(destSessions.length);
            transcodingProcList = new ArrayList<>(destSessions.length);

            for (int i = 0; i < destSessions.length; ++i) {
                System.err.println("  - Open RTP session for addr: " + destSessions[i].getIpAddr() + " port: " + destSessions[i].getPort() + " ttl: " + destSessions[i].getTtl());

                RTPManager rtpMngr = RTPManager.newInstance();
                rtpMngr.addSessionListener(new RTPManagerSessionListener());
                rtpMngr.addReceiveStreamListener(this);
                rtpMngr.addRemoteListener(new RTPManagerRemoteEventListener() {
                    @Override
                    public void onSenderReportEvent(SenderReportEvent evt, SenderReportData reportData) {
                        String cname = evt.getSessionManager().getLocalParticipant().getCNAME();
                        StatisticsData.DATA.countReportData(cname, reportData);
                    }
                });

                destIpAddr = InetAddress.getByName(destSessions[i].getIpAddr());
                if (destIpAddr.isMulticastAddress()) {
                    // local and remote address pairs are identical:
                    localAddr = new SessionAddress(destIpAddr,
                            destSessions[i].getPort(),
                            destSessions[i].getTtl());
                    destAddr = new SessionAddress(destIpAddr,
                            destSessions[i].getPort(),
                            destSessions[i].getTtl());
                } else {
                    localAddr = new SessionAddress(InetAddress.getByName(bindSessions[i].getIpAddr()), bindSessions[i].getPort());
                    destAddr = new SessionAddress(destIpAddr, destSessions[i].getPort());
                }
                System.err.println("Session local addr: " + localAddr);
                System.err.println("Session remote addr: " + destAddr);

                SourceDescription[] srcDescList = JMFUtils.createSourceDescriptions(localAddr);
                JMFUtils.initializeRTPManager(rtpMngr, localAddr, srcDescList);

                // You can try out some other buffer size to see
                // if you can get better smoothness.
                BufferControl buffCtrl = (BufferControl) rtpMngr.getControl("javax.media.control.BufferControl");
                if (buffCtrl != null) {
                    buffCtrl.setBufferLength(GlobalOptionHelper.getBuffCtrlLength());
                    buffCtrl.setEnabledThreshold(true);
                    buffCtrl.setMinimumThreshold(GlobalOptionHelper.getBuffCtrlMin());
                    System.err.printf("Setup Buffer Control: buffLen[%d] minThreshold[%d] in ms unit.%n", buffCtrl.getBufferLength(), buffCtrl.getMinimumThreshold());
                }

                rtpMngr.addTarget(destAddr);
                rtpMngrs.add(rtpMngr);
            }
        } catch (Exception e) {
            System.err.println("Cannot create the RTP Session: " + e.getMessage());
            return false;
        }

        StatisticsData.DATA.addInstance();

        // Wait for stream to arrive before moving on.
        long begin = System.currentTimeMillis();
        long waitingPeriod = 300000;  // wait for a maximum of 5 mins.

        try {
            synchronized (receivedStreamCount) {
                while (receivedStreamCount.get() < destSessions.length &&
                        System.currentTimeMillis() - begin < waitingPeriod) {
                    if (receivedStreamCount.get() < destSessions.length) {
                        System.err.println("  - Waiting for RTP data stream to arrive...");
                    }
                    receivedStreamCount.wait(1000);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to wait for RTP data stream : " + e.getMessage());
        }

        if (receivedStreamCount.get() < destSessions.length) {
            System.err.println("No All RTP data streams was received.");
            close();
            return false;
        }

        if (!switchReceiveStreams()) {
            System.err.println("Switch received streams failed.");
            close();
            return false;
        }

        return true;
    }

    /**
     * Close the session managers.
     */
    public void close() {
        // close the RTP session.
        List<RTPManager> rtpMngrList;
        List<Processor> procList;
        synchronized (this) {
            rtpMngrList = rtpMngrs;
            procList = transcodingProcList;
            rtpMngrs = null;
            transcodingProcList = null;
        }

        if (rtpMngrList == null) {
            return ;
        }

        if (rtpMngrList.size() == destSessions.length) {
            StatisticsData.DATA.decreaseInstance();
        }

        for (Processor processor : procList) {
            processor.stop();
            processor.close();
            processor.deallocate();
        }

        for (RTPManager mngr : rtpMngrList) {
            mngr.removeTargets("Closing session from RTPSwitch.");
            mngr.dispose();
        }
    }

    public List<String> getLocalCNAMEs() {
        List<String> cnames = new ArrayList<>(rtpMngrs.size());
        for (RTPManager mngr : rtpMngrs) {
            cnames.add(mngr.getLocalParticipant().getCNAME());
        }

        return cnames;
    }

    protected boolean switchReceiveStreams() {
        Set<Map.Entry<RTPManager, ReceiveStream>> entrySet = receivedStreamsMap.entrySet();

        boolean result = true;
        for (Map.Entry<RTPManager, ReceiveStream> entry : entrySet) {
            result = result && transmitStream(entry.getKey(), entry.getValue());
        }

        receivedStreamsMap.clear();
        return result;
    }

    protected boolean transmitStream(RTPManager fromMngr, ReceiveStream recvStream) {
        try {
            RTPManager toMngr = findOppositeEnd(fromMngr);
            DataSource dataSource = recvStream.getDataSource();
            if (GlobalOptionHelper.shouldTranscoding()) {
                dataSource = createTranscodingProcessor(dataSource);
            }

            SendStream sendStream = toMngr.createSendStream(dataSource, 0);
            sendStream.start();
        } catch (Exception e) {
            System.err.println("Transmit stream failed: " + e.getMessage());
            return false;
        }

        return true;
    }

    protected RTPManager findOppositeEnd(RTPManager mngr) {
        if (mngr == null) {
            return null;
        }

        int oppositeIndex = -1;
        for (int i = 0; i < rtpMngrs.size(); ++i) {
            if (rtpMngrs.get(i) != mngr) {
                oppositeIndex = i;
                break;
            }
        }

        if (oppositeIndex == -1) {
            return null;
        } else {
            return rtpMngrs.get(oppositeIndex);
        }
    }

    protected DataSource createTranscodingProcessor(DataSource origDS) throws IOException, NoProcessorException {
        if (origDS == null) {
            throw new IllegalArgumentException("OrigDataSource for transcoding is null!");
        }

        Processor processor = Manager.createProcessor(origDS);
        StateHelper stateHelper = new StateHelper(processor);
        // Wait for it to configure
        if (!stateHelper.configure()) {
            throw  new IllegalStateException("Couldn't configure processor");
        }

        // Get the tracks from the processor
        TrackControl[] tracks = processor.getTrackControls();
        // Do we have at least one track?
        if (tracks == null || tracks.length < 1) {
            throw new IllegalStateException("Couldn't find tracks in processor");
        }

        // Set the output content descriptor to RAW_RTP
        // This will limit the supported formats reported from
        // Track.getSupportedFormats to only valid RTP formats.
        ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
        processor.setContentDescriptor(cd);

        Format supported[];
        String targetEncoding = AudioFormat.GSM_RTP;
        boolean atLeastOneTrack = false;
        // Program the tracks.
        for (int i = 0; i < tracks.length; ++i) {
            if (tracks[i].isEnabled()) {
                supported = tracks[i].getSupportedFormats();

                // We've set the output content to the RAW_RTP.
                // So all the supported formats should work with RTP.
                // We'll just pick the first one.
                if (supported.length > 0) {
                    Format chosen = null;
                    for (Format fmt : supported) {
                        if (fmt instanceof AudioFormat && fmt.isSameEncoding(targetEncoding)) {
                            tracks[i].setFormat(fmt);
                            chosen = fmt;
                            break;
                        }
                    }

                    if (chosen != null) {
                        atLeastOneTrack = true;
                        System.err.println("Track " + i + " is set to transmit as:");
                        System.err.println("  " + chosen);
                    } else {
                        tracks[i].setEnabled(false);
                    }
                } else {
                    tracks[i].setEnabled(false);
                }
            } else {
                tracks[i].setEnabled(false);
            }
        }

        if (!atLeastOneTrack) {
            throw new IllegalStateException("Couldn't set any of the tracks to a valid RTP format");
        }

        if (!stateHelper.realize()) {
            throw new IllegalStateException("Couldn't realize processor");
        }

        processor.start();
        transcodingProcList.add(processor);
        return processor.getDataOutput();
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
            RTPManager mngr = (RTPManager)receiveStreamEvent.getSource();
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

                receivedStreamsMap.put(mngr, stream);

                // Notify init() that a new stream had arrived.
                synchronized (receivedStreamCount) {
                    receivedStreamCount.incrementAndGet();
                    receivedStreamCount.notifyAll();
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
            close();
        } else if (receiveStreamEvent instanceof TimeoutEvent) {
            System.err.println("  - Receive timeout from: " + participant.getCNAME());
            close();
        }
    }

    class RTPManagerSessionListener implements SessionListener {
        @Override
        public void update(SessionEvent sessionEvent) {
            if (sessionEvent instanceof NewParticipantEvent) {
                Participant p = ((NewParticipantEvent) sessionEvent).getParticipant();
                System.err.println("  - A new participant had just joined: " + p.getCNAME());
            }
        }
    }
}
