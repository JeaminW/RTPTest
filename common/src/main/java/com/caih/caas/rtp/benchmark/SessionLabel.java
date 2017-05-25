package com.caih.caas.rtp.benchmark;

/**
 * Created by jeaminw on 17/5/4.
 * A utility class to parse the session addresses.
 */
public class SessionLabel {

    private String ipAddr = "";
    private int port;
    private int ttl = 0;

    public SessionLabel(String session) throws IllegalArgumentException {
        if (session == null || session.length() <= 0) {
            throw new IllegalArgumentException("Insufficient parameters.");
        }

        String[] tokens = session.split("/");
        if (tokens.length < 2) {
            throw new IllegalArgumentException("Insufficient parameters.");
        }

        ipAddr = tokens[0];
        String portStr = tokens[1];
        String ttlStr = tokens.length > 2 ? tokens[2] : null;

        if (ipAddr.equals("")) {
            throw new IllegalArgumentException();
        }

        if (!portStr.equals("")) {
            try {
                Integer integer = Integer.valueOf(portStr);
                if (integer != null) {
                    port = integer.intValue();
                }
            } catch (Throwable t) {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException();
        }

        if (ttlStr != null) {
            try {
                Integer integer = Integer.valueOf(ttlStr);
                if (integer != null) {
                    ttl = integer.intValue();
                }
            } catch (Throwable t) {
                throw new IllegalArgumentException();
            }
        }
    }

    public SessionLabel(String ipAddr, int port, int ttl) {
        if (ipAddr == null) { throw new IllegalArgumentException("Arg[ipAddr] could't be null"); }
        if (port <= 0) { throw new IllegalArgumentException("Arg[port] could't be less than or equal to zero"); }
        if (ttl < 0) { throw new IllegalArgumentException("Arg[ttl] could't be less than zero"); }

        this.ipAddr = ipAddr;
        this.port = port;
        this.ttl = ttl;
    }

    public SessionLabel(String ipAddr, int port) {
        this(ipAddr, port, 0);
    }

    public SessionLabel labelWithPortOffset(int portOffset) {
        return new SessionLabel(ipAddr, port+portOffset, ttl);
    }

    public static SessionLabel[] labelsWithPortOffset(SessionLabel[] labels,  int portOffset) {
        if (labels == null) {
            return null;
        }

        SessionLabel[] newLabels = new SessionLabel[labels.length];
        for (int i = 0; i < labels.length; ++i) {
            if (labels[i] == null) {
                continue;
            }

            newLabels[i] = labels[i].labelWithPortOffset(portOffset);
        }

        return newLabels;
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder(ipAddr);
        string.append("/").append(port);
        string.append("/").append(ttl);

        return string.toString();
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public int getPort() {
        return port;
    }

    public int getTtl() {
        return ttl;
    }
}
