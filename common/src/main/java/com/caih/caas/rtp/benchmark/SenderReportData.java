package com.caih.caas.rtp.benchmark;

/**
 * Created by jeaminw on 17/5/23.
 */
public interface SenderReportData {
    long getPktSentTotal();

    /**
     * 获取总丢包率%，已换算为%单位
     * @return 总丢包率%
     */
    double getPktLostRateTotal();

    double getPktLostRate();

    double getPktJitter();
}
