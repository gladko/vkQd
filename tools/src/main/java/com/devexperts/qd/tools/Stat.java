package com.devexperts.qd.tools;


import com.devexperts.qd.monitoring.ConnectorsMonitoringTask;
import com.devexperts.qd.stats.QDStats;

import java.util.concurrent.atomic.AtomicLong;

public class Stat {
    final QDStats rootStat;
    final ConnectorsMonitoringTask connectorsStat;

    final AtomicLong prev = new AtomicLong();
    final AtomicLong counter = new AtomicLong();

    Stat() {
        rootStat = new QDStats();
        rootStat.initRoot(QDStats.SType.ANY, VkNetTest.SCHEME);

        connectorsStat = new ConnectorsMonitoringTask(rootStat);
    }

    public long diff() {
        long value = counter.get();
        long tmp = prev.getAndSet(value);
        return value - tmp;
    }
}
