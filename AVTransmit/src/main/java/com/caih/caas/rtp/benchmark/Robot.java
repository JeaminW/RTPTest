package com.caih.caas.rtp.benchmark;

import org.jgroups.JChannel;
import org.jgroups.blocks.RpcDispatcher;

import javax.media.MediaLocator;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jeaminw on 17/5/24.
 */
public class Robot {
    JChannel channel;
    RpcDispatcher dispatcher;

    String props;
    Config config;
    int port;
    List<AVTransmit2> instances = new LinkedList<>();
    boolean isExit;

    public Robot(String props, Config config) {
        this.props = props;
        this.config = config;
    }

    public void start(String clusterName) throws Exception {
        if (channel != null) {
            return ;
        }

        port = config.getBindSession().getPort();

        channel = new JChannel(); // default props
        channel.setDiscardOwnMessages(true);
        dispatcher = new RpcDispatcher(channel, this);
        channel.connect(clusterName);

        isExit = false;
        synchronized (this) {
            while (!isExit) {
                this.wait(1000);
            }
        }
    }

    public void stop() {
        if (channel != null) {
            channel.close();
            dispatcher.stop();

            channel = null;
            dispatcher = null;
        }
    }

    public void exit() {
        synchronized (this) {
            isExit = true;
            this.notifyAll();
        }
    }

    public String openSession(String remoteSession) throws UnknownHostException {
        String localIPAddr = config.getBindSession().getIpAddr();
        InetAddress localAddr = InetAddress.getByName(localIPAddr);
        int port = Utils.findLocalRTPPortsFromBasePort(localAddr, this.port);

        SessionLabel localSession = new SessionLabel(localIPAddr, port);
        SessionLabel destSession = new SessionLabel(remoteSession);
        this.port = port + 2;

        // Start the transmission
        final AVTransmit2 trans = new AVTransmit2(config.getMediaLocator(), localSession, destSession);
        String result = trans.start();
        instances.add(trans);

        // result will be non-null if there was an error. The return
        // value is a String describing the possible error. Print it.
        if (result != null) {
            System.err.println("Error : " + result);
            throw new IllegalStateException(result);
        }

        return localSession.toString();
    }

    public static void main(String[] args) {
        GlobalOptionHelper.preferIPv4Stack();

        try {
            Robot robot = new Robot(null, null);
            robot.start("rpc-test");
            robot.stop();
        }
        catch(Exception ex) {
            System.err.println(ex);
        }
    }

    public interface Config {
        MediaLocator getMediaLocator();
        SessionLabel getBindSession();
    }
}
