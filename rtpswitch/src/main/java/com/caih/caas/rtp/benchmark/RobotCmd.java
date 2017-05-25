package com.caih.caas.rtp.benchmark;

import org.apache.commons.cli.*;

/**
 * Created by jeaminw on 17/5/5.
 */
public class RobotCmd {
    private static final String OPT_SHORT_BIND_ADDR = "b";
    private static final String OPT_SHORT_TRANSCODING = "t";
    private static final String OPT_SHORT_HELP = "h";
    private static final String OPT_SHORT_VERBOSE = "v";

    private static final ConfigImpl config = new ConfigImpl();

    public static void main(String argv[]) {
        GlobalOptionHelper.preferIPv4Stack();

        Options options = buildOptions();
        parseOptions(options, argv);

        try {
            Robot robot = new Robot(null, config);
            robot.start(GlobalOptionHelper.MulticastClusterName);
            robot.test();
            robot.stop();
        } catch (Exception ex) {
            System.err.println(ex);
        }

        System.err.println("Exiting RTPSwitch");
    }

    private static Options buildOptions() {
        Options options = new Options();
        options.addOption(Option.builder(OPT_SHORT_BIND_ADDR)
                .required()
                .hasArg()
                .desc("binded local session address, ip/port/ttl")
                .build());

        options.addOption(OPT_SHORT_TRANSCODING, "turn on transcoding process");
        options.addOption(OPT_SHORT_HELP, "help", false, "print help for the command.");
        options.addOption(OPT_SHORT_VERBOSE, "print report data");

        return options;
    }

    private static void parseOptions(Options options, String[] argv) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, argv);

            config.bindSession = new SessionLabel(cmd.getOptionValue(OPT_SHORT_BIND_ADDR));

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
            System.err.println("Parse command line error." + e);
            printHelp(options);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( RTPSwitch.class.getSimpleName() + " -b <BIND-SESSION-ADDR> [-t] [-h] [-v]", options);

        System.exit(0);
    }

    static class ConfigImpl implements Robot.Config {
        private SessionLabel bindSession;

        @Override
        public SessionLabel getBindSession() {
            return bindSession;
        }
    }
}
