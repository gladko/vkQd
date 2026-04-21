package intro.qd;

import com.devexperts.qd.QDTicker;
import com.devexperts.qd.qtp.*;
import com.devexperts.qd.stats.QDStats;

import java.util.ArrayList;
import java.util.List;


public class DemoMux {

    public static void main(String[] args) throws InterruptedException {
        QDTicker ticker = Util.createTicker(QDStats.VOID);

        List<MessageConnector> connectors = new ArrayList<>();
        // server socket for pushing data into MUX. Client-producer must connect to this socket.
        MessageAdapter.AbstractFactory distributorFactory =
                new DistributorAdapter.Factory(ticker, null, null, null);
        connectors.addAll(MessageConnectors.createMessageConnectors(distributorFactory,
                        ":7000", QDStats.VOID));

        // Server socket for pulling data from MUX. Client-consumer must connect to this socket.
        AgentAdapter.Factory factory = new AgentAdapter.Factory(ticker, null, null, null);
        connectors.addAll(MessageConnectors.createMessageConnectors(factory,
                ":8000", QDStats.VOID));

        connectors.forEach(MessageConnectorMBean::start);

        Thread.sleep(Long.MAX_VALUE);
    }
}
