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

    private static double packetLossRateMin = 0.1;
    private static int packetSentMin = 2000;
    private static int packetLossSessionMax = 20;

    public static void load(String path) throws IOException {
        Properties props = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(path);
            props.load(input);

            if (props.containsKey(CFG_PktLossRateMin)) {
                packetLossRateMin = Double.parseDouble(props.getProperty(CFG_PktLossRateMin));
            }

            if (props.containsKey(CFG_PktSentMin)) {
                packetSentMin = Integer.parseInt(props.getProperty(CFG_PktSentMin));
            }

            if (props.containsKey(CFG_PktLossSessionMax)) {
                packetLossSessionMax = Integer.parseInt(props.getProperty(CFG_PktLossSessionMax));
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
}
