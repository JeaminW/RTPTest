package com.caih.caas.rtp.benchmark;

/**
 * Created by jeaminw on 17/5/4.
 * A utility class to parse the session addresses.
 */
public class SessionLabel {

    private String ipAddr;
    private int port;
    private int ttl = 1;

    SessionLabel(String session) throws IllegalArgumentException {
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
