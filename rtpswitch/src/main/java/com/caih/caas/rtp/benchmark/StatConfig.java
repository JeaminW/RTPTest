package com.caih.caas.rtp.benchmark;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by jeaminw on 17/5/26.
 */
public class StatConfig {
    public static final String CFG_PktLossRateMin = "cfg.stat.pktLossRateMin";
    public static final String CFG_PktSentMin = "cfg.stat.pktSentMin";
    public static final String CFG_PktLossSessionMax = "cfg.stat.pktLossSessionMax";
    public static final String CFG_PktTransferStatCheckTimes = "cfg.stat.pktTransferStatCheckTimes";

    private static double packetLossRateMin = 1;
    private static int packetSentMin = 1000;
    private static int packetLossSessionMax = 20;
    private static int packetTransferStatCheckTimes = 10;

    public static void load(String path) throws IOException {
        Properties props = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(path);
            props.load(input);

            if (props.containsKey(CFG_PktLossRateMin)) {
                packetLossRateMin = Double.parseDouble(props.getProperty(CFG_PktLossRateMin));
                if (packetLossRateMin > 1.0D || packetLossRateMin < 0.0D) {
                    throw new IllegalArgumentException(CFG_PktLossRateMin + " must between 0.0 ~ 1.0");
                }
            }

            if (props.containsKey(CFG_PktSentMin)) {
                packetSentMin = Integer.parseInt(props.getProperty(CFG_PktSentMin));
                if (packetSentMin < 0) {
                    throw new IllegalArgumentException(CFG_PktSentMin + " must greater than or equal to 0");
                }
            }

            if (props.containsKey(CFG_PktLossSessionMax)) {
                packetLossSessionMax = Integer.parseInt(props.getProperty(CFG_PktLossSessionMax));
                if (packetLossSessionMax < 0) {
                    throw new IllegalArgumentException(CFG_PktLossSessionMax + " must greater than or equal to 0");
                }
            }

            if (props.containsKey(CFG_PktTransferStatCheckTimes)) {
                packetTransferStatCheckTimes = Integer.parseInt(props.getProperty(CFG_PktTransferStatCheckTimes));
                if (packetTransferStatCheckTimes < 0) {
                    throw new IllegalArgumentException(CFG_PktTransferStatCheckTimes + " must greater than or equal to 0");
                }
            }

            System.err.println("Load stat config: ");
            System.err.println(props);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static double getPacketLossRateMin() {
        return packetLossRateMin;
    }

    public static int getPacketSentMin() {
        return packetSentMin;
    }

    public static int getPacketLossSessionMax() {
        return packetLossSessionMax;
    }

    public static int getPacketTransferStatCheckTimes() {
        return packetTransferStatCheckTimes;
    }
}
