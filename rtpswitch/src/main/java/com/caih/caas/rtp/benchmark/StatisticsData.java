package com.caih.caas.rtp.benchmark;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jeaminw on 17/5/5.
 */
public enum StatisticsData {
    DATA();

    private AtomicInteger liveInstancesCnt = new AtomicInteger(0);

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

}
