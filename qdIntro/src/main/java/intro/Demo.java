package intro;

import com.devexperts.qd.*;
import com.devexperts.qd.kit.DefaultRecord;
import com.devexperts.qd.kit.DefaultScheme;
import com.devexperts.qd.ng.*;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.util.WideDecimal;
import com.dxfeed.api.impl.DXFeedScheme;

import java.util.concurrent.ThreadLocalRandom;


public class Demo {
    static final DefaultScheme scheme = DXFeedScheme.getInstance();
    private static final DefaultRecord QUOTE = scheme.findRecordByName("Quote");
    private static final int QUOTE_BID_PRICE_INDEX = QUOTE.findFieldByName("Quote.Bid.Price").getIndex();


    public static void main(String[] args) throws InterruptedException {
        QDTicker ticker = createTicker(QDStats.VOID);

        Producer logicA = new Producer(ticker);
        new Thread(logicA::publishQuotes).start();

        new Consumer(ticker);
        Thread.sleep(Long.MAX_VALUE);
    }

    public static class Producer {
        QDDistributor qdDistributor;

        Producer(QDTicker ticker) {
            qdDistributor = ticker.distributorBuilder().build();
        }

        void publishQuotes() {
            while (true) {
                String symbol = "IBM";
                RecordBuffer buffer = RecordBuffer.getInstance();
                RecordCursor cur = buffer.add(QUOTE, scheme.getCodec().encode(symbol), symbol);
                double price = ThreadLocalRandom.current().nextDouble(100);
                cur.setLong(QUOTE_BID_PRICE_INDEX, WideDecimal.composeWide(price));
                qdDistributor.process(buffer);
                buffer.release();
            }
        }
    }

    public static class Consumer {
        QDAgent qdAgent;

        Consumer(QDTicker ticker) {
            qdAgent = ticker.agentBuilder().build();

            qdAgent.setRecordListener(this::onDataAvailable);

            String symbol = "IBM";
            RecordBuffer buffer = RecordBuffer.getInstance(RecordMode.SUBSCRIPTION);
            buffer.add(QUOTE, scheme.getCodec().encode(symbol), symbol);
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
    }

    public static QDTicker createTicker(QDStats stats) {
        return QDFactory.getDefaultFactory()
                .tickerBuilder()
                .withScheme(scheme)
                .withStats(stats)
                .build();
    }
}
