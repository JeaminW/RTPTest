package com.caih.caas.rtp.benchmark;

import org.apache.commons.cli.*;

import javax.media.MediaLocator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by jeaminw on 17/5/8.
 */
public class Main {
    private static final String OPT_SHORT_MEDIA = "m";
    private static final String OPT_SHORT_BIND_ADDR = "b";
    private static final String OPT_SHORT_DEST_ADDR = "d";
    private static final String OPT_SHORT_RUN = "r";
    private static final String OPT_SHORT_HELP = "h";
    private static final String OPT_SHORT_GUI = "g";
    private static final String OPT_SHORT_VERBOSE = "v";

    private static MediaLocator mediaLocator;
    private static SessionLabel bindSession;
    private static SessionLabel destSession;
    private static int numOfInstances = 1;
    private static List<AVTransmit2> instances;

    public static void main(String[] args) {
        Options options = buildOptions();
        parseOptions(options, args);

        instances = new ArrayList<>(numOfInstances);
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < numOfInstances; ++i) {
            SessionLabel bindSession = Main.bindSession.labelWithPortOffset(i * 2);
            SessionLabel destSession = Main.destSession.labelWithPortOffset(i * 2);

            // Create a audio transmit object with the specified params.
            final AVTransmit2 trans = new AVTransmit2(mediaLocator, bindSession, destSession);
            instances.add(trans);
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    // Start the transmission
                    String result = trans.start();

                    // result will be non-null if there was an error. The return
                    // value is a String describing the possible error. Print it.
                    if (result != null) {
                        System.err.println("Error : " + result);
                        System.exit(0);
                    }
                }
            });
        }

        try {
            cachedThreadPool.shutdown();
            cachedThreadPool.awaitTermination(10, TimeUnit.MINUTES);
            cachedThreadPool = null;
            System.err.println("Start transmission for 4 minutes...");
            
            Thread.currentThread().sleep(240000);
        } catch (InterruptedException ie) {
        }

        // Stop the transmission

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
        options.addOption(OPT_SHORT_GUI, "gui", false, "play received audio stream with gui.");
        options.addOption(OPT_SHORT_VERBOSE, "print report data");

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

            if (cmd.hasOption(OPT_SHORT_GUI)) {
                GlobalOptionHelper.setShowGUI(true);
            }

            if (cmd.hasOption(OPT_SHORT_VERBOSE)) {
                GlobalOptionHelper.setRTPReportVerbose(true);
            }
        } catch (ParseException e) {
            System.err.println("Parse command line error.");
            printHelp(options);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( AVTransmit2.class.getSimpleName() + " -m <MEDIA-URL> -b <BIND-SESSION-ADDR> -d <DEST-SESSION-ADDR> [-r runs] [-h] [-g/--gui] [-v]", options);

        System.exit(0);
    }
}
