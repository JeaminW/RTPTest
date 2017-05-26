package com.caih.caas.rtp.benchmark;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.MembershipListener;
import org.jgroups.View;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.util.RspList;
import org.jgroups.util.Tuple;
import org.jgroups.util.Util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Created by jeaminw on 17/5/24.
 */
public class Robot implements MembershipListener {
    JChannel channel;
    RpcDispatcher dispatcher;

    String props;
    Config config;
    int port;
    List<RTPSwitch> instances = new LinkedList<>();
    Multiset<Address> peerSessionStat = HashMultiset.create(4);
    Object memberCountLock = new Object();

    private static final long RPC_TIMEOUT = 60000;

    public Robot(String props, Config config) {
        this.props = props;
        this.config = config;
    }

    /** MembershipListener */
    @Override
    public void viewAccepted(View new_view) {
        System.err.println("new view: " + new_view);

        synchronized (memberCountLock) {
            memberCountLock.notifyAll();
        }
    }

    @Override
    public void suspect(Address suspected_mbr) {
        System.err.println(suspected_mbr + " is suspected");
    }

    @Override
    public void block() { }

    @Override
    public void unblock() { }

    public void start(String clusterName) throws Exception {
        if (channel != null) {
            return ;
        }

        port = config.getBindSession().getPort();

        channel = new JChannel(); // default props
        channel.setDiscardOwnMessages(true);
        dispatcher = new RpcDispatcher(channel, this);
        channel.connect(clusterName);
    }

    public void stop() {
        if (channel != null) {
            channel.close();
            dispatcher.stop();

            channel = null;
            dispatcher = null;
        }

        if (instances != null) {
            for (RTPSwitch rtpSwitch : instances) {
                rtpSwitch.close();
            }

            instances.clear();
            instances = null;
        }
    }

    public void test() throws Exception {
        try {
            do {
                synchronized (memberCountLock) {
                    while (channel.getView().size() < 3) {
                        memberCountLock.wait(1000);
                    }
                }

                RTPSwitch rtpSwitch = setupOneSwitch();
                List<String> localCNAMEs = rtpSwitch.getLocalCNAMEs();
                int transferOK;
                do {
                    Util.sleep(1000);

                    transferOK = 0;
                    for (String cname : localCNAMEs) {
                        SenderReportData reportData = StatisticsData.DATA.getReportData(cname);
                        if (reportData != null && reportData.getPktSentTotal() > StatConfig.getPacketSentMin()) {
                            ++transferOK;
                        }
                    }
                } while (transferOK < localCNAMEs.size());

                System.out.println(StatisticsData.DATA.statDataSummaryLine());
            } while (StatisticsData.DATA.packetLossSessionCount() < StatConfig.getPacketLossSessionMax());
        } finally {
            sendExitCmd();
        }
    }

    RTPSwitch setupOneSwitch() throws Exception {
        SessionLabel[] localSessions = generateLocalSessions();
        Address[] peers = pickTwoPeers();
        SessionLabel[] destSessions = new SessionLabel[] { requestOpenSession(peers[0], localSessions[0]), requestOpenSession(peers[1], localSessions[1]) };

        final RTPSwitch rtpSwitch = new RTPSwitch(localSessions, destSessions);
        if (!rtpSwitch.init()) {
            throw new IllegalStateException("Failed to initialize the sessions.");
        }

        instances.add(rtpSwitch);
        peerSessionStat.add(peers[0]);
        peerSessionStat.add(peers[1]);

        return rtpSwitch;
    }

    SessionLabel[] generateLocalSessions() throws UnknownHostException {
        final int SESSION_COUNT = 2;
        String localIPAddr = config.getBindSession().getIpAddr();
        InetAddress localAddr = InetAddress.getByName(localIPAddr);
        int port = Utils.findLocalRTPPortsFromBasePort(localAddr, this.port, SESSION_COUNT);
        if (port <= 0) {
            throw new IllegalStateException("No available ports.");
        }

        this.port = port + 2 * SESSION_COUNT;
        return new SessionLabel[] { new SessionLabel(localIPAddr, port), new SessionLabel(localIPAddr, port+2) };
    }

    Address[] pickTwoPeers() {
        View view = channel.getView();
        List<Address> members = view.getMembers();
        Address localAddr = channel.getAddress();

        List<Tuple<Address, Integer>> orderedMembers = new ArrayList<>(members.size());
        for (Address address : members) {
            if (address.equals(localAddr)) {
                continue;
            }

            int count = peerSessionStat.count(address);
            orderedMembers.add(new Tuple<>(address, Integer.valueOf(count)));
        }

        Collections.sort(orderedMembers, new Comparator<Tuple<Address, Integer>>() {
            @Override
            public int compare(Tuple<Address, Integer> o1, Tuple<Address, Integer> o2) {
                if (o1.getVal1().equals(o2.getVal1())) {
                    return 0;
                }

                return o1.getVal2().compareTo(o2.getVal2());
            }
        });

        return new Address[] { orderedMembers.get(0).getVal1(), orderedMembers.get(1).getVal1() };
    }

    SessionLabel requestOpenSession(Address peer, SessionLabel session) throws Exception {
        if (peer == null || session == null) {
            throw new IllegalArgumentException("peer and session arguments could't be null.");
        }

        String result = dispatcher.callRemoteMethod(peer, "openSession", new Object[] { session.toString() }, new Class[] { String.class }, new RequestOptions(ResponseMode.GET_ALL, RPC_TIMEOUT));
        if (result == null) {
            throw new IllegalStateException("request OpenSession no result.");
        }

        return new SessionLabel(result);
    }

    RspList sendExitCmd() throws Exception {
        return dispatcher.callRemoteMethods(null, "exit", null, null, new RequestOptions(ResponseMode.GET_ALL, 0));
    }

    public static void main(String[] args) {
        GlobalOptionHelper.preferIPv4Stack();

        try {
            Robot robot = new Robot(null, null);
            robot.start("rpc-test");
            robot.test();
            robot.stop();
        }
        catch(Exception ex) {
            System.err.println(ex);
        }
    }

    public interface Config {
        SessionLabel getBindSession();
    }
}
