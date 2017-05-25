package com.caih.caas.rtp.benchmark;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * Created by jeaminw on 17/5/24.
 */
public class Utils {
    public static int findLocalRTPPorts(InetAddress localAddr, int sessionCount) {
        boolean found = false;
        int port = -1;

        if (sessionCount < 1) {
            return port;
        }

        while(!found) {
            do {
                double num = Math.random();
                port = (int)(num * 65535.0D);
                if(port % 2 != 0) {
                    ++port;
                }
            } while(port < 1024 || port > '\ufffe');

            try {
                for (int i = 0; i < sessionCount; ++i) {
                    int dataPort = port + i * 2;
                    DatagramSocket datagramSocket = new DatagramSocket(dataPort, localAddr);
                    datagramSocket.close();
                    datagramSocket = new DatagramSocket(dataPort + 1, localAddr);
                    datagramSocket.close();
                }
                found = true;
            } catch (SocketException ex) {
                found = false;
            }
        }

        return port;
    }

    public static int findLocalRTPPorts(InetAddress localAddr) {
        return findLocalRTPPorts(localAddr, 1);
    }

    public static int findLocalRTPPorts() {
        return findLocalRTPPorts(null);
    }

    public static int findLocalRTPPortsFromBasePort(InetAddress localAddr, final int baseport, final int sessionCount) {
        final int INVALID_PORT = -1;
        boolean found = false;

        if (baseport < 1 || sessionCount < 1) {
            return INVALID_PORT;
        }

        int port = baseport;
        if (port % 2 != 0) {
            ++port;
        }

        while (!found) {
            if (port + (sessionCount - 1) * 2 > 65534) {
                port = INVALID_PORT;
                break;
            }

            int dataPort = 0;
            try {
                for (int i = 0; i < sessionCount; ++i) {
                    dataPort = port + i * 2;
                    DatagramSocket datagramSocket = new DatagramSocket(dataPort, localAddr);
                    datagramSocket.close();
                    datagramSocket = new DatagramSocket(dataPort + 1, localAddr);
                    datagramSocket.close();
                }
                found = true;
            } catch (SocketException ex) {
                port = dataPort + 2;
                found = false;
            }
        }

        return port;
    }

    public static int findLocalRTPPortsFromBasePort(InetAddress localAddr, final int baseport) {
        return findLocalRTPPortsFromBasePort(localAddr, baseport, 1);
    }
}
