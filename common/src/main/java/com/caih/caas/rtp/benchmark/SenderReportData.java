package com.caih.caas.rtp.benchmark;

/**
 * Created by jeaminw on 17/5/23.
 */
public interface SenderReportData {
    double getPktLostRateTotal();

    double getPktLostRate();

    double getPktJitter();
}
