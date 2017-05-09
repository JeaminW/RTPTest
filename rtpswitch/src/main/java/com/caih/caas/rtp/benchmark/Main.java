package com.caih.caas.rtp.benchmark;

import org.apache.commons.cli.*;

/**
 * Created by jeaminw on 17/5/5.
 */
public class Main {
    private static final String OPT_SHORT_BIND_IP_ADDR = "b";
    private static final String OPT_SHORT_RUN = "r";
    private static final String OPT_SHORT_HELP = "h";

    private static String bindIPAddr;
    private static String[] sessions;
    private static int numOfInstances = 1;

    public static void main(String argv[]) {
        Options options = buildOptions();
        parseOptions(options, argv);

        RTPSwitch rtpSwitch = new RTPSwitch(bindIPAddr, sessions);
        if (!rtpSwitch.init()) {
            System.err.println("Failed to initialize the sessions.");
            System.exit(-1);
        }

        // Check to see if RTPSwitch is done.
        try {
            while (StatisticsData.DATA.getLiveInstancesCount() > 0) {
                System.err.println("Current alive sessions : " + StatisticsData.DATA.getLiveInstancesCount());
                Thread.sleep(1000);
            }
        } catch (Exception e) {
        }

        System.err.println("Exiting RTPSwitch");
    }

    private static Options buildOptions() {
        Options options = new Options();
        options.addOption(Option.builder(OPT_SHORT_BIND_IP_ADDR)
                .required()
                .hasArg()
                .desc("binded local IP address")
                .build());

        options.addOption(OPT_SHORT_RUN, true, "number of running instances");
        options.addOption(OPT_SHORT_HELP, "help", false, "print help for the command.");

        return options;
    }

    private static void parseOptions(Options options, String[] argv) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, argv);

            Main.bindIPAddr = cmd.getOptionValue(OPT_SHORT_BIND_IP_ADDR);

            if (cmd.hasOption(OPT_SHORT_RUN)) {
                String strNumOfInstances = cmd.getOptionValue(OPT_SHORT_RUN);
                int numOfInstances = Integer.parseInt(strNumOfInstances);
                if (numOfInstances > 1) {
                    Main.numOfInstances = numOfInstances;
                }
            }

            if (cmd.hasOption(OPT_SHORT_HELP)) {
                printHelp(options);
            }

            String[] sessions = cmd.getArgs();
            if (sessions.length != 2) {
                System.err.println("Must specify two destination sessions.");
                printHelp(options);
            } else {
                Main.sessions = sessions;
            }
        } catch (ParseException e) {
            System.err.println("Parse command line error.");
            printHelp(options);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "RTPSwitch [OPTION] <SESSION-A> <SESSION-B>", "", options, "<SESSION-A/B> : <destIpAddr>/<port>/<ttl>");

        System.exit(0);
    }
}
