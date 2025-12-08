package intro.qd;

import com.devexperts.qd.QDDistributor;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordCursor;
import com.devexperts.qd.qtp.AgentAdapter;
import com.devexperts.qd.qtp.MessageAdapter;
import com.devexperts.qd.qtp.MessageConnectors;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.util.WideDecimal;

import java.util.concurrent.ThreadLocalRandom;
import static intro.qd.SingleProcessDemo.*;

public class ClientProducer {
    private final QDDistributor qdDistributor;

    public ClientProducer(QDTicker ticker) {
        qdDistributor = ticker.distributorBuilder().build();
    }

    public void publishQuotes(String symbol) {
        while (true) {
            try {
                RecordBuffer buffer = RecordBuffer.getInstance();
                RecordCursor cur = buffer.add(QUOTE, SCHEME.getCodec().encode(symbol), symbol);
                double price = ThreadLocalRandom.current().nextDouble(100);
                cur.setLong(QUOTE_BID_PRICE_INDEX, WideDecimal.composeWide(price));
                qdDistributor.process(buffer);
                buffer.release();
                Thread.sleep(300);
            } catch (Exception e) {
                System.out.println(e);
                throw new RuntimeException(e);
            }
        }
    }


    public static void main(String[] args) throws InterruptedException {
        QDTicker ticker = SingleProcessDemo.createTicker(QDStats.VOID);
        MessageAdapter.Factory distAdapter =
                new AgentAdapter.Factory(ticker, null, null, null);

        MessageConnectors.startMessageConnectors(
                MessageConnectors.createMessageConnectors(
                        MessageConnectors.applicationConnectionFactory(distAdapter),
//                        "127.0.0.1:7000")
                        ":8000")
//                        "(:8000[bindAddr=km1.test])(:8000[bindAddr=km2.test])(:8000[bindAddr=localhost])")
        );

        ClientProducer producer = new ClientProducer(ticker);
        producer.publishQuotes("IBM");
        Thread.sleep(Long.MAX_VALUE);
    }
}
