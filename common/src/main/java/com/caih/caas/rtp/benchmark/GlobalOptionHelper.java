package com.caih.caas.rtp.benchmark;

/**
 * Created by jeaminw on 17/5/23.
 */
public class GlobalOptionHelper {
    public static final String OPT_VERBOSE = "rtp.report.verbose";

    public static boolean isRTPReportVerbose() {
        return Boolean.getBoolean(OPT_VERBOSE);
    }

    public static void setRTPReportVerbose(boolean isVerbose) {
        System.setProperty(OPT_VERBOSE, Boolean.toString(isVerbose));
    }
}
