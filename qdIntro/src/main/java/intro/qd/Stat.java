package intro.qd;

import com.devexperts.qd.DataScheme;
import com.devexperts.qd.monitoring.ConnectorsMonitoringTask;
import com.devexperts.qd.stats.QDStats;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Stat {
    final QDStats rootStat;
    final ConnectorsMonitoringTask connectorsStat;;

    public Stat(QDStats.SType type, DataScheme scheme) {
        rootStat = new QDStats(type, scheme);
        connectorsStat = new ConnectorsMonitoringTask(rootStat);

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(connectorsStat, 0, 10, TimeUnit.SECONDS);
    }
}
