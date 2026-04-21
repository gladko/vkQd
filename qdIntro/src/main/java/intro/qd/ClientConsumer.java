package intro.qd;

import com.devexperts.qd.QDAgent;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.ng.*;
import com.devexperts.qd.qtp.DistributorAdapter;
import com.devexperts.qd.qtp.MessageAdapter;
import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.util.WideDecimal;


import java.util.List;

import static intro.qd.SingleProcessDemo.*;

public class ClientConsumer {
    private final QDTicker ticker;
    private final QDAgent qdAgent;
    private final Stat stat;

    public ClientConsumer(Stat stat, QDTicker ticker) {
        this.stat = stat;
        this.ticker = ticker;
        qdAgent = ticker.agentBuilder().build();
        qdAgent.setRecordListener(this::onDataAvailable);
    }

    private List<MessageConnector> connect(String address) {
        MessageAdapter.Factory distAdapter =
                new DistributorAdapter.Factory(ticker, null, null, null);

        return Util.connect(address, distAdapter, stat);
    }

    public void subscribeQuotes(String... symbols) {
        RecordBuffer buffer = RecordBuffer.getInstance(RecordMode.SUBSCRIPTION);
        for (String symbol : symbols) {
            buffer.add(QUOTE, Util.SCHEME.getCodec().encode(symbol), symbol);
//            add.setEventFlags(EventFlag.REMOVE_SYMBOL);
        }
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
        buffer.release();
    }

    public static void main(String[] args) throws InterruptedException {
        Stat stat = new Stat(QDStats.SType.ANY, Util.SCHEME);
        QDTicker ticker = Util.createTicker(stat.rootStat.create(QDStats.SType.TICKER));
        ClientConsumer consumer = new ClientConsumer(stat, ticker);

//        consumer.connect("km1.test,km2.test,localhost:8000[connectOrder=ordered]");
        List<MessageConnector> connectors = consumer.connect("localhost:8000");
        consumer.subscribeQuotes("IBM");

        reportLags(connectors);

        Thread.sleep(Long.MAX_VALUE);
    }

    // example of access to QdStats via API.
    private static void reportLags(List<MessageConnector> connectors) {
        for (MessageConnector connector : connectors) {
            System.out.println("IO_DATA_READ_LAGS: " + connector.getStats().getValue(QDStats.SValue.IO_DATA_READ_LAGS));
            System.out.println("IO_DATA_WRITE_LAGS: " + connector.getStats().getValue(QDStats.SValue.IO_DATA_WRITE_LAGS));
        }
    }
}
