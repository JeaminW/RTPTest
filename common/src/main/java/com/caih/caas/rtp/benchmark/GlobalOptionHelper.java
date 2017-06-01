package com.caih.caas.rtp.benchmark;

/**
 * Created by jeaminw on 17/5/23.
 */
public class GlobalOptionHelper {
    public static final String OPT_VERBOSE = "rtp.report.verbose";
    public static final String OPT_ShowGUI = "rtp.media.play";
    public static final String OPT_TRANSCODING = "rtp.media.transcoding";
    public static final String OPT_PreferIPv4Stack = "java.net.preferIPv4Stack";
    public static final String OPT_BuffCtrlLength = "rtp.buffctrl.length";
    public static final String OPT_BuffCtrlMin = "rtp.buffctrl.min";

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

    public static long getBuffCtrlLength() {
        return Long.getLong(OPT_BuffCtrlLength, 350);
    }

    public static void setBuffCtrlLength(long length) {
        System.setProperty(OPT_BuffCtrlLength, String.valueOf(length));
    }

    public static long getBuffCtrlMin() {
        return Long.getLong(OPT_BuffCtrlMin, 100);
    }

    public static void setBuffCtrlMin(long min) {
        System.setProperty(OPT_BuffCtrlMin, String.valueOf(min));
    }
}
