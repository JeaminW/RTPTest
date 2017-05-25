package com.caih.caas.rtp.benchmark;

import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by jeaminw on 17/5/5.
 */
public class Main {
    private static final String OPT_SHORT_BIND_ADDR = "b";
    private static final String OPT_SHORT_DEST_ADDR = "d";
    private static final String OPT_SHORT_RUN = "r";
    private static final String OPT_SHORT_TRANSCODING = "t";
    private static final String OPT_SHORT_HELP = "h";
    private static final String OPT_SHORT_VERBOSE = "v";

    private static SessionLabel[] bindSessions;
    private static SessionLabel[] destSessions;
    private static int numOfInstances = 1;
    private static List<RTPSwitch> instances;

    public static void main(String argv[]) {
        Options options = buildOptions();
        parseOptions(options, argv);

        instances = new ArrayList<>(numOfInstances);
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < numOfInstances; ++i) {
            SessionLabel[] bindSessions = SessionLabel.labelsWithPortOffset(Main.bindSessions, i * 2);
            SessionLabel[] destSessions = SessionLabel.labelsWithPortOffset(Main.destSessions, i * 2);

            final RTPSwitch rtpSwitch = new RTPSwitch(bindSessions, destSessions);
            instances.add(rtpSwitch);
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    if (!rtpSwitch.init()) {
                        System.err.println("Failed to initialize the sessions.");
                        System.exit(-1);
                    }
                }
            });
        }

        // Check to see if RTPSwitch is done.
        try {
            cachedThreadPool.shutdown();
            cachedThreadPool.awaitTermination(10, TimeUnit.MINUTES);
            cachedThreadPool = null;
            System.err.println("All RTPSwitch instances were initialized.");

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
        options.addOption(Option.builder(OPT_SHORT_BIND_ADDR)
                .required()
                .numberOfArgs(2)
                .desc("binded local session addr, ip/port/ttl")
                .build());

        options.addOption(Option.builder(OPT_SHORT_DEST_ADDR)
                .required()
                .numberOfArgs(2)
                .desc("destination session addr, ip/port/ttl")
                .build());

        options.addOption(OPT_SHORT_RUN, true, "number of running instances");
        options.addOption(OPT_SHORT_TRANSCODING, "turn on transcoding process");
        options.addOption(OPT_SHORT_HELP, "help", false, "print help for the command.");
        options.addOption(OPT_SHORT_VERBOSE, "print report data");

        return options;
    }

    private static void parseOptions(Options options, String[] argv) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, argv);

            String[] sessions = cmd.getOptionValues(OPT_SHORT_BIND_ADDR);
            if (sessions.length != 2) {
                System.err.println("Must specify two binded local sessions.");
                printHelp(options);
            } else {
                bindSessions = new SessionLabel[]{new SessionLabel(sessions[0]), new SessionLabel(sessions[1])};
            }

            sessions = cmd.getOptionValues(OPT_SHORT_DEST_ADDR);
            if (sessions.length != 2) {
                System.err.println("Must specify two destination sessions.");
                printHelp(options);
            } else {
                destSessions = new SessionLabel[]{new SessionLabel(sessions[0]), new SessionLabel(sessions[1])};
            }

            if (cmd.hasOption(OPT_SHORT_RUN)) {
                String strNumOfInstances = cmd.getOptionValue(OPT_SHORT_RUN);
                int numOfInstances = Integer.parseInt(strNumOfInstances);
                if (numOfInstances > 1) {
                    Main.numOfInstances = numOfInstances;
                }
            }

            if (cmd.hasOption(OPT_SHORT_TRANSCODING)) {
                GlobalOptionHelper.setTranscoding(true);
            }

            if (cmd.hasOption(OPT_SHORT_HELP)) {
                printHelp(options);
            }

            if (cmd.hasOption(OPT_SHORT_VERBOSE)) {
                GlobalOptionHelper.setRTPReportVerbose(true);
            }
        } catch (Exception e) {
            System.err.println("Parse command line error.");
            printHelp(options);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( RTPSwitch.class.getSimpleName() + " -b <BIND-SESSION-ADDR> <BIND-SESSION-ADDR> -d <DEST-SESSION-ADDR> <DEST-SESSION-ADDR> [-r runs] [-t] [-h] [-v]", options);

        System.exit(0);
    }
}
