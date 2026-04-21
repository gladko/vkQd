package intro.qd;

import com.devexperts.qd.QDDistributor;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordCursor;
import com.devexperts.qd.qtp.AgentAdapter;
import com.devexperts.qd.qtp.MessageAdapter;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.util.WideDecimal;

import java.util.concurrent.ThreadLocalRandom;
import static intro.qd.SingleProcessDemo.*;

public class ClientProducer {
    private final QDTicker ticker;
    private final QDDistributor qdDistributor;
    private final Stat stat;

    public ClientProducer(Stat stat, QDTicker ticker) {
        this.stat = stat;
        this.ticker = ticker;
        qdDistributor = ticker.distributorBuilder().build();
    }

    private void connect(String address) {
        MessageAdapter.Factory distAdapter =
                new AgentAdapter.Factory(ticker, null, null, null);
        Util.connect(address, distAdapter, stat);
    }

    public void publishQuotes(String symbol) {
        while (true) {
            try {
                RecordBuffer buffer = RecordBuffer.getInstance();
                RecordCursor cur = buffer.add(QUOTE, Util.SCHEME.getCodec().encode(symbol), symbol);
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
        Stat stat = new Stat(QDStats.SType.ANY, Util.SCHEME);
        QDTicker ticker = Util.createTicker(stat.rootStat.create(QDStats.SType.TICKER));
        ClientProducer producer = new ClientProducer(stat, ticker);
        producer.connect(":8000");
//        producer.connect("127.0.0.1:7000");
//        producer.connect("(:8000[bindAddr=km1.test])(:8000[bindAddr=km2.test])(:8000[bindAddr=localhost])");

        producer.publishQuotes("IBM");
        Thread.sleep(Long.MAX_VALUE);
    }

}
