package com.caih.caas.rtp.benchmark;

import org.apache.commons.cli.*;

import javax.media.MediaLocator;

/**
 * Created by jeaminw on 17/5/8.
 */
public class Main {
    private static final String OPT_SHORT_MEDIA = "m";
    private static final String OPT_SHORT_BIND_ADDR = "b";
    private static final String OPT_SHORT_DEST_ADDR = "d";
    private static final String OPT_SHORT_RUN = "r";
    private static final String OPT_SHORT_HELP = "h";

    private static MediaLocator mediaLocator;
    private static SessionLabel bindSession;
    private static SessionLabel destSession;
    private static int numOfInstances = 1;

    public static void main(String[] args) {
        Options options = buildOptions();
        parseOptions(options, args);

        // Create a audio transmit object with the specified params.
        AVTransmit2 at = new AVTransmit2(mediaLocator, bindSession, destSession);
        // Start the transmission
        String result = at.start();

        // result will be non-null if there was an error. The return
        // value is a String describing the possible error. Print it.
        if (result != null) {
            System.err.println("Error : " + result);
            System.exit(0);
        }

        System.err.println("Start transmission for 4 minutes...");

        try {
            Thread.currentThread().sleep(240000);
        } catch (InterruptedException ie) {
        }

        // Stop the transmission
        at.stop();
        System.err.println("...transmission ended.");
    }

    private static Options buildOptions() {
        Options options = new Options();
        options.addOption(Option.builder(OPT_SHORT_MEDIA)
                .required()
                .hasArg()
                .desc("input media URL or file name")
                .build());

        options.addOption(Option.builder(OPT_SHORT_BIND_ADDR)
                .required()
                .hasArg()
                .desc("binded local session addr, ip/port/ttl")
                .build());

        options.addOption(Option.builder(OPT_SHORT_DEST_ADDR)
                .required()
                .hasArg()
                .desc("multicast, broadcast or unicast destination session address for the transmission, ip/port/ttl")
                .build());

        options.addOption(OPT_SHORT_RUN, true, "number of running instances");
        options.addOption(OPT_SHORT_HELP, "help", false, "print help for the command.");

        return options;
    }

    private static void parseOptions(Options options, String[] argv) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, argv);

            mediaLocator = new MediaLocator(cmd.getOptionValue(OPT_SHORT_MEDIA));
            bindSession = new SessionLabel(cmd.getOptionValue(OPT_SHORT_BIND_ADDR));
            destSession = new SessionLabel(cmd.getOptionValue(OPT_SHORT_DEST_ADDR));

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
        } catch (ParseException e) {
            System.err.println("Parse command line error.");
            printHelp(options);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( AVTransmit2.class.getSimpleName() + " -m <MEDIA-URL> -b <BIND-SESSION-ADDR> -d <DEST-SESSION-ADDR> [-r runs] [-h]", options);

        System.exit(0);
    }
}
