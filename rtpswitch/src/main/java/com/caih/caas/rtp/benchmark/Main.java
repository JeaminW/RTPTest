package com.caih.caas.rtp.benchmark;

import java.util.Arrays;

/**
 * Created by jeaminw on 17/5/5.
 */
public class Main {

    public static void main(String argv[]) {
        if (argv.length < 3) {
            printUsage();
        }

        RTPSwitch rtpSwitch = new RTPSwitch(argv[0], Arrays.copyOfRange(argv, 1, argv.length));
        if (!rtpSwitch.init()) {
            System.err.println("Failed to initialize the sessions.");
            System.exit(-1);
        }

        // Check to see if RTPSwitch is done.
        try {
            while (StatisticsData.DATA.getLiveInstancesCount() > 0) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
        }

        System.err.println("Exiting RTPSwitch");
    }

    static void printUsage() {
        System.err.println("Usage: RTPSwitch <bindIPAddr> <session> <session>");
        System.err.println("     <bindIPAddr>: Bind local IP address");
        System.err.println("     <session>: <address>/<port>/<ttl>");

        System.exit(0);
    }
}
