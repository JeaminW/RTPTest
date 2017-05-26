package com.caih.caas.rtp.benchmark;

import java.util.Formatter;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jeaminw on 17/5/5.
 */
public enum StatisticsData {
    DATA();

    private AtomicInteger liveInstancesCnt = new AtomicInteger(0);
    private Map<String, SenderReportData> reportDataMap = new ConcurrentHashMap<>(500);
    private Map<String, SenderReportData> pktLossDataMap = new ConcurrentHashMap<>(30);

    public int getLiveInstancesCount() {
        return liveInstancesCnt.get();
    }

    public int addInstance() {
        return liveInstancesCnt.incrementAndGet();
    }

    public int addInstances(int amount) {
        return liveInstancesCnt.addAndGet(amount);
    }

    public int decreaseInstance() {
        return liveInstancesCnt.decrementAndGet();
    }

    public int decreaseInstances(int amount) {
        return liveInstancesCnt.addAndGet(-amount);
    }

    public void countReportData(String cname, SenderReportData reportData) {
        if (reportData == null) {
            return ;
        }

        reportDataMap.put(cname, reportData);

        if (reportData.getPktSentTotal() > StatConfig.getPacketSentMin() && reportData.getPktLostRateTotal() > StatConfig.getPacketLossRateMin()) {
            pktLossDataMap.put(cname, reportData);
        }
    }

    public SenderReportData getReportData(String cname) {
        return reportDataMap.get(cname);
    }

    public String statDataSummaryLine() {
        Formatter formatter = new Formatter();
        formatter.format("LiveInst: %d", liveInstancesCnt.get());
        formatter.format("; Record report data sessions: %d", reportDataMap.size());
        formatter.format("; Pkt loss sessions: %d", pktLossDataMap.size());

        return formatter.toString();
    }

    public int packetLossSessionCount() {
        return pktLossDataMap.size();
    }

    public String packetLossSessionsSummary() {
        StringBuilder string = new StringBuilder();
        string.append(statDataSummaryLine()).append("\n");

        Set<Map.Entry<String, SenderReportData>> entrySet = pktLossDataMap.entrySet();
        for (Map.Entry<String, SenderReportData> entry : entrySet) {
            string.append(entry.getKey()).append(" >> ").append(entry.getValue()).append("\n");
        }

        return string.toString();
    }
}
