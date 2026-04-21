package intro.qd;

import com.devexperts.qd.QDFactory;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.kit.DefaultScheme;
import com.devexperts.qd.qtp.MessageAdapter;
import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectors;
import com.devexperts.qd.stats.QDStats;
import com.dxfeed.api.impl.DXFeedScheme;

import java.util.List;

public class Util {
    static final DefaultScheme SCHEME = DXFeedScheme.getInstance();

    public static QDTicker createTicker(QDStats stats) {
        return QDFactory.getDefaultFactory()
                .tickerBuilder()
                .withScheme(SCHEME)
                .withStats(stats)
                .build();
    }

    public static List<MessageConnector> connect(String address, MessageAdapter.Factory maFactory, Stat stat) {
        List<MessageConnector> connectors = MessageConnectors.createMessageConnectors(
                MessageConnectors.applicationConnectionFactory(maFactory),
                address, stat.rootStat);
        MessageConnectors.startMessageConnectors(connectors);
        stat.connectorsStat.addConnectors(connectors);
        return connectors;
    }
}
