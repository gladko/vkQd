package intro;

import com.devexperts.qd.*;
import com.devexperts.qd.kit.DefaultRecord;
import com.devexperts.qd.kit.DefaultScheme;
import com.devexperts.qd.stats.QDStats;
import com.dxfeed.api.impl.DXFeedScheme;


public class SingleProcessDemo {
    static final DefaultScheme SCHEME = DXFeedScheme.getInstance();
    static final DefaultRecord QUOTE = SCHEME.findRecordByName("Quote");
    static final int QUOTE_BID_PRICE_INDEX = QUOTE.findFieldByName("Quote.Bid.Price").getIndex();


    public static void main(String[] args) throws InterruptedException {
        QDTicker ticker = createTicker(QDStats.VOID);

        String symbol = "IBM";
        ClientProducer logicA = new ClientProducer(ticker);
        new Thread(() -> logicA.publishQuotes(symbol)).start();

        ClientConsumer logicB = new ClientConsumer(ticker);
        logicB.subscribeQuotes(symbol);
        Thread.sleep(Long.MAX_VALUE);
    }


    public static QDTicker createTicker(QDStats stats) {
        return QDFactory.getDefaultFactory()
                .tickerBuilder()
                .withScheme(SCHEME)
                .withStats(stats)
                .build();
    }
}
