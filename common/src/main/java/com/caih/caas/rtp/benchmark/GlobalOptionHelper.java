package com.caih.caas.rtp.benchmark;

/**
 * Created by jeaminw on 17/5/23.
 */
public class GlobalOptionHelper {
    public static final String OPT_VERBOSE = "rtp.report.verbose";
    public static final String OPT_ShowGUI = "rtp.media.play";
    public static final String OPT_TRANSCODING = "rtp.media.transcoding";
    public static final String OPT_PreferIPv4Stack = "java.net.preferIPv4Stack";

    public static final String MulticastClusterName = "RTP-Test";

    public static boolean isRTPReportVerbose() {
        return Boolean.getBoolean(OPT_VERBOSE);
    }

    public static void setRTPReportVerbose(boolean isVerbose) {
        System.setProperty(OPT_VERBOSE, Boolean.toString(isVerbose));
    }

    public static boolean isShowGUI() {
        return Boolean.getBoolean(OPT_ShowGUI);
    }

    public static void setShowGUI(boolean show) {
        System.setProperty(OPT_ShowGUI, Boolean.toString(show));
    }

    public static boolean shouldTranscoding() {
        return Boolean.getBoolean(OPT_TRANSCODING);
    }

    public static void setTranscoding(boolean transcoding) {
        System.setProperty(OPT_TRANSCODING, Boolean.toString(transcoding));
    }

    public static void preferIPv4Stack() {
        System.setProperty(OPT_PreferIPv4Stack, Boolean.TRUE.toString());
    }
}
