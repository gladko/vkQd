package intro;

import com.devexperts.qd.QDTicker;
import com.devexperts.qd.qtp.DistributorAdapter;
import com.devexperts.qd.qtp.MessageAdapter;
import com.devexperts.qd.qtp.MessageConnectors;
import com.devexperts.qd.stats.QDStats;

public class ClientConsumer {
    public static void main(String[] args) throws InterruptedException {
        QDTicker ticker = Demo.createTicker(QDStats.VOID);
        MessageAdapter.Factory distAdapter =
                new DistributorAdapter.Factory(ticker, null, null, null);

        MessageConnectors.startMessageConnectors(
                MessageConnectors.createMessageConnectors(
                        MessageConnectors.applicationConnectionFactory(distAdapter),
                        "127.0.0.1:8000")
        );

        new Demo.Consumer(ticker);

        Thread.sleep(Long.MAX_VALUE);
    }

}
