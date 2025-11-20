package intro.qd;

import com.devexperts.qd.QDAgent;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.ng.*;
import com.devexperts.qd.qtp.DistributorAdapter;
import com.devexperts.qd.qtp.MessageAdapter;
import com.devexperts.qd.qtp.MessageConnectors;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.util.WideDecimal;

import static intro.qd.SingleProcessDemo.*;

public class ClientConsumer {
    private final QDAgent qdAgent;

    public ClientConsumer(QDTicker ticker) {
        qdAgent = ticker.agentBuilder().build();
        qdAgent.setRecordListener(this::onDataAvailable);
    }

    public void subscribeQuotes(String symbol) {
        RecordBuffer buffer = RecordBuffer.getInstance(RecordMode.SUBSCRIPTION);
        buffer.add(QUOTE, SCHEME.getCodec().encode(symbol), symbol);
//            add.setEventFlags(EventFlag.REMOVE_SYMBOL);
        qdAgent.setSubscription(buffer);
        buffer.release();
    }

    private void onDataAvailable(RecordProvider recordProvider) {
        RecordBuffer buffer = RecordBuffer.getInstance(RecordMode.DATA);
        recordProvider.retrieve(buffer);

        buffer.retrieve(new AbstractRecordSink() {
            @Override
            public void append(RecordCursor cursor) {
                System.out.println(cursor);
                System.out.println(WideDecimal.toDouble(cursor.getLong(QUOTE_BID_PRICE_INDEX)));
            }
        });
    }

    public static void main(String[] args) throws InterruptedException {
        QDTicker ticker = SingleProcessDemo.createTicker(QDStats.VOID);
        MessageAdapter.Factory distAdapter =
                new DistributorAdapter.Factory(ticker, null, null, null);

        MessageConnectors.startMessageConnectors(
                MessageConnectors.createMessageConnectors(
                        MessageConnectors.applicationConnectionFactory(distAdapter),
                        "127.0.0.1:8000")
        );

        ClientConsumer consumer = new ClientConsumer(ticker);
        consumer.subscribeQuotes("IBM");

        Thread.sleep(Long.MAX_VALUE);
    }

}
