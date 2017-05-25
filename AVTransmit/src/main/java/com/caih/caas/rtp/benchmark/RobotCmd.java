package com.caih.caas.rtp.benchmark;

import org.apache.commons.cli.*;

import javax.media.MediaLocator;

/**
 * Created by jeaminw on 17/5/8.
 */
public class RobotCmd {
    private static final String OPT_SHORT_MEDIA = "m";
    private static final String OPT_SHORT_BIND_ADDR = "b";
    private static final String OPT_SHORT_HELP = "h";
    private static final String OPT_SHORT_GUI = "g";
    private static final String OPT_SHORT_VERBOSE = "v";

    private static final ConfigImpl config = new ConfigImpl();

    public static void main(String[] args) {
        GlobalOptionHelper.preferIPv4Stack();

        Options options = buildOptions();
        parseOptions(options, args);

        try {
            Robot robot = new Robot(null, config);
            robot.start(GlobalOptionHelper.MulticastClusterName);
            robot.stop();
        } catch (Exception ex) {
            System.err.println(ex);
        }
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
                .desc("binded local session address, ip/port/ttl")
                .build());

        options.addOption(OPT_SHORT_HELP, "help", false, "print help for the command.");
        options.addOption(OPT_SHORT_GUI, "gui", false, "play received audio stream with gui.");
        options.addOption(OPT_SHORT_VERBOSE, "print report data");

        return options;
    }

    private static void parseOptions(Options options, String[] argv) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, argv);

            config.mediaLocator = new MediaLocator(cmd.getOptionValue(OPT_SHORT_MEDIA));
            config.bindSession = new SessionLabel(cmd.getOptionValue(OPT_SHORT_BIND_ADDR));

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
        formatter.printHelp( AVTransmit2.class.getSimpleName() + " -m <MEDIA-URL> -b <BIND-SESSION-ADDR> [-h] [-g/--gui] [-v]", options);

        System.exit(0);
    }

    static class ConfigImpl implements Robot.Config {
        private MediaLocator mediaLocator;
        private SessionLabel bindSession;

        @Override
        public MediaLocator getMediaLocator() {
            return mediaLocator;
        }

        @Override
        public SessionLabel getBindSession() {
            return bindSession;
        }
    }
}
