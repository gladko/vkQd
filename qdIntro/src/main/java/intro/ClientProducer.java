package intro;

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
import static intro.SingleProcessDemo.*;

public class ClientProducer {
    private final QDDistributor qdDistributor;

    public ClientProducer(QDTicker ticker) {
        qdDistributor = ticker.distributorBuilder().build();
    }

    public void publishQuotes(String symbol) {
        while (true) {
            RecordBuffer buffer = RecordBuffer.getInstance();
            RecordCursor cur = buffer.add(QUOTE, SCHEME.getCodec().encode(symbol), symbol);
            double price = ThreadLocalRandom.current().nextDouble(100);
            cur.setLong(QUOTE_BID_PRICE_INDEX, WideDecimal.composeWide(price));
            qdDistributor.process(buffer);
            buffer.release();
        }
    }


    public static void main(String[] args) throws InterruptedException {
        QDTicker ticker = SingleProcessDemo.createTicker(QDStats.VOID);
        MessageAdapter.Factory distAdapter =
                new AgentAdapter.Factory(ticker, null, null, null);

        MessageConnectors.startMessageConnectors(
                MessageConnectors.createMessageConnectors(
                        MessageConnectors.applicationConnectionFactory(distAdapter),
                        "127.0.0.1:7000")
//                        ":8000")
        );

        ClientProducer producer = new ClientProducer(ticker);
        new Thread(() -> producer.publishQuotes("IBM")).start();

        Thread.sleep(Long.MAX_VALUE);
    }
}
