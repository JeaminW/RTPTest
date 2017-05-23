package com.caih.caas.rtp.benchmark;

import javax.media.rtp.RemoteListener;
import javax.media.rtp.event.RemoteEvent;
import javax.media.rtp.event.SenderReportEvent;
import javax.media.rtp.rtcp.Feedback;
import javax.media.rtp.rtcp.SenderReport;
import java.util.Formatter;
import java.util.Vector;

/**
 * Created by jeaminw on 17/5/17.
 */
public class RTPManagerRemoteEventListener implements RemoteListener {

//    public void onReceiverReportEvent(ReceiverReportEvent evt) {
//    }

//    public void onRemoteCollisionEvent(RemoteCollisionEvent evt) {
//    }

    public void onSenderReportEvent(SenderReportEvent evt, SenderReportData reportData) {
    }

    @Override
    public void update(RemoteEvent remoteEvent) {
        if (remoteEvent instanceof SenderReportEvent) {
            SenderReportEvent senderReportEvt = (SenderReportEvent) remoteEvent;
            SenderReport report = senderReportEvt.getReport();
            Vector feedbacks = report.getFeedbackReports();
            Feedback feedback = (feedbacks != null && feedbacks.size() > 0) ? (Feedback) feedbacks.get(0) : null;
            SenderReportDataImpl reportData = null;

            if (feedback != null) {
                reportData = new SenderReportDataImpl();
                reportData.pktLostRateTotal = (double) feedback.getNumLost() / (feedback.getNumLost() + report.getSenderPacketCount()) * 100.0D;
                reportData.pktLostRate = feedback.getFractionLost() / 256.0D * 100.0D;
                reportData.pktJitter = feedback.getJitter() / 8000.0D * 1000;

                if (GlobalOptionHelper.isRTPReportVerbose()) {
                    Formatter formatter = new Formatter();
                    formatter.format("  - SR[%s] report:", report.getParticipant().getCNAME());
                    formatter.format(" PktLostRateTotal[%d/%d %.3f%%]", feedback.getNumLost(), report.getSenderPacketCount(), reportData.pktLostRateTotal);
                    formatter.format(" PktLostRateSR[%.3f%%]", reportData.pktLostRate);
                    formatter.format(" PktJitterSR[%.1fms]", reportData.pktJitter);

                    System.err.println(formatter.toString());
                }
            }

            onSenderReportEvent((SenderReportEvent) remoteEvent, reportData);
        }
    }

    private class SenderReportDataImpl implements SenderReportData {
        private double pktLostRateTotal;
        private double pktLostRate;
        private double pktJitter;

        @Override
        public double getPktLostRateTotal() {
            return pktLostRateTotal;
        }

        @Override
        public double getPktLostRate() {
            return pktLostRate;
        }

        @Override
        public double getPktJitter() {
            return pktJitter;
        }
    }
}
