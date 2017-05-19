package com.caih.caas.rtp.benchmark;

import javax.media.rtp.GlobalReceptionStats;
import javax.media.rtp.GlobalTransmissionStats;
import javax.media.rtp.RTPManager;
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
    @Override
    public void update(RemoteEvent remoteEvent) {
        if (remoteEvent instanceof SenderReportEvent) {
            SenderReportEvent senderReportEvt = (SenderReportEvent) remoteEvent;
            SenderReport report = senderReportEvt.getReport();
            Vector feedbacks = report.getFeedbackReports();
            Feedback feedback = (feedbacks != null && feedbacks.size() > 0) ? (Feedback) feedbacks.get(0) : null;

            if (feedback != null) {
                double pktLostRateSRTotal = (double) feedback.getNumLost() / (feedback.getNumLost() + report.getSenderPacketCount()) * 100.0D;
                double pktLostRateSR = feedback.getFractionLost() / 256.0D * 100.0D;
                double pktJitterSR = feedback.getJitter() / 8000.0D * 1000;

                Formatter formatter = new Formatter();
                formatter.format("  - SR[%s] report:", report.getParticipant().getCNAME());
                formatter.format(" PktLostRateTotal[%d/%d %.3f%%]", feedback.getNumLost(), report.getSenderPacketCount(), pktLostRateSRTotal);
                formatter.format(" PktLostRateSR[%.3f%%]", pktLostRateSR);
                formatter.format(" PktJitterSR[%.1fms]", pktJitterSR);

                System.err.println(formatter.toString());
            }
        }
    }
}
