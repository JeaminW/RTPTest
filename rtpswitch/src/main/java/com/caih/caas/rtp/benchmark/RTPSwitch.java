package com.caih.caas.rtp.benchmark;

import javax.media.control.BufferControl;
import javax.media.protocol.DataSource;
import javax.media.rtp.*;
import javax.media.rtp.event.*;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jeaminw on 17/5/5.
 */
public class RTPSwitch implements ReceiveStreamListener {
    String bindIpAddr;
    String[] sessions;
    List<RTPManager> rtpMngrs;

    Map<RTPManager, ReceiveStream> receivedStreamsMap = new ConcurrentHashMap<>(2);
    final AtomicInteger receivedStreamCount = new AtomicInteger(0);

    public RTPSwitch(String bindIpAddr, String[] sessions) {
        this.bindIpAddr = bindIpAddr;
        this.sessions = sessions;
    }

    public boolean init() {
        if (sessions == null || sessions.length != 2) {
            throw new IllegalArgumentException("Needs 2 sessions: " + sessions);
        }

        try {
            if (bindIpAddr == null || bindIpAddr.length() <= 0) {
                bindIpAddr = InetAddress.getLocalHost().getHostAddress();
            }

            InetAddress destIpAddr;
            SessionAddress localAddr;
            SessionAddress destAddr;
            SessionLabel session;
            rtpMngrs = new ArrayList<>(sessions.length);

            for (int i = 0; i < sessions.length; ++i) {
                // Parse the session addresses.
                try {
                    session = new SessionLabel(sessions[i]);
                } catch (IllegalArgumentException e) {
                    System.err.println("Failed to parse the session address given: " + sessions[i]);
                    return false;
                }

                System.err.println("  - Open RTP session for addr: " + session.getIpAddr() + " port: " + session.getPort() + " ttl: " + session.getTtl());

                RTPManager rtpMngr = RTPManager.newInstance();
                rtpMngr.addSessionListener(new RTPManagerSessionListener());
                rtpMngr.addReceiveStreamListener(this);

                destIpAddr = InetAddress.getByName(session.getIpAddr());
                if (destIpAddr.isMulticastAddress()) {
                    // local and remote address pairs are identical:
                    localAddr = new SessionAddress(destIpAddr,
                            session.getPort(),
                            session.getTtl());
                    destAddr = new SessionAddress(destIpAddr,
                            session.getPort(),
                            session.getTtl());
                } else {
                    localAddr = new SessionAddress(InetAddress.getByName(bindIpAddr), session.getPort());
                    destAddr = new SessionAddress(destIpAddr, session.getPort());
                }
                System.err.println("Session local addr: " + localAddr);
                System.err.println("Session remote addr: " + destAddr);

                rtpMngr.initialize(localAddr);

                // You can try out some other buffer size to see
                // if you can get better smoothness.
                BufferControl buffCtrl = (BufferControl) rtpMngr.getControl("javax.media.control.BufferControl");
                if (buffCtrl != null) {
                    buffCtrl.setBufferLength(350);
                    buffCtrl.setEnabledThreshold(true);
                    buffCtrl.setMinimumThreshold(20);
                    System.err.println("Setup Buffer Control: buffLen[350] minThreshold[20] in ms unit.");
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
                while (receivedStreamCount.get() < sessions.length &&
                        System.currentTimeMillis() - begin < waitingPeriod) {
                    if (receivedStreamCount.get() < sessions.length) {
                        System.err.println("  - Waiting for RTP data stream to arrive...");
                    }
                    receivedStreamCount.wait(1000);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to wait for RTP data stream : " + e.getMessage());
        }

        if (receivedStreamCount.get() < sessions.length) {
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
    protected void close() {
        // close the RTP session.
        List<RTPManager> rtpMngrList = null;
        synchronized (this) {
            rtpMngrList = rtpMngrs;
            rtpMngrs = null;
        }

        if (rtpMngrList == null) {
            return ;
        }

        if (rtpMngrList.size() == sessions.length) {
            StatisticsData.DATA.decreaseInstance();
        }

        for (RTPManager mngr : rtpMngrList) {
            mngr.removeTargets("Closing session from RTPSwitch.");
            mngr.dispose();
        }
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
