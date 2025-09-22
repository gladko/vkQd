package intro;

import com.devexperts.qd.QDTicker;
import com.devexperts.qd.qtp.AgentAdapter;
import com.devexperts.qd.qtp.MessageAdapter;
import com.devexperts.qd.qtp.MessageConnectors;
import com.devexperts.qd.stats.QDStats;

public class ClientProducer {
    public static void main(String[] args) throws InterruptedException {
        QDTicker ticker = Demo.createTicker(QDStats.VOID);
        MessageAdapter.Factory distAdapter =
                new AgentAdapter.Factory(ticker, null, null, null);

        MessageConnectors.startMessageConnectors(
                MessageConnectors.createMessageConnectors(
                        MessageConnectors.applicationConnectionFactory(distAdapter),
                        "127.0.0.1:7000")
//                        ":8000")
        );

        Demo.Producer logicA = new Demo.Producer(ticker);
        new Thread(logicA::publishQuotes).start();

        Thread.sleep(Long.MAX_VALUE);
    }

}
