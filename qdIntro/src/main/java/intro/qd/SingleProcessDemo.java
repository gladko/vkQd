package intro.qd;

import com.devexperts.qd.*;
import com.devexperts.qd.kit.DefaultRecord;
import com.devexperts.qd.stats.QDStats;


public class SingleProcessDemo {
    static final DefaultRecord QUOTE = Util.SCHEME.findRecordByName("Quote");
    static final int QUOTE_BID_PRICE_INDEX = QUOTE.findFieldByName("Quote.Bid.Price").getIndex();


    public static void main(String[] args) throws InterruptedException {
        Stat stat = new Stat(QDStats.SType.ANY, Util.SCHEME);
        QDTicker ticker = Util.createTicker(stat.rootStat);

        String symbol = "IBM";
        ClientProducer logicA = new ClientProducer(stat, ticker);
        new Thread(() -> logicA.publishQuotes(symbol)).start();

        ClientConsumer logicB = new ClientConsumer(stat, ticker);
        logicB.subscribeQuotes(symbol);
        Thread.sleep(Long.MAX_VALUE);
    }
}
