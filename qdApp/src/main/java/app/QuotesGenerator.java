package app;

import com.devexperts.qd.QDDistributor;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordCursor;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.util.WideDecimal;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static app.Util.*;

public class QuotesGenerator {
    private static final AtomicLong calcIterations = new AtomicLong();
    private static final AtomicLong counter = new AtomicLong();

    public static void main(String[] args) throws InterruptedException {
        QDTicker ticker = Util.createTicker(QDStats.VOID);
        Util.startAgentConnector(ticker, ":9000");

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        QuotesGenerator quotesGenerator = new QuotesGenerator(ticker);
        executorService.scheduleWithFixedDelay(
                quotesGenerator::printStat, 0, REPORT_PERIOD, TimeUnit.SECONDS);

        quotesGenerator.publishQuotes();

        Thread.sleep(Long.MAX_VALUE);
    }

    QDDistributor qdDistributor;
    SubscriptionHolder subscriptionHolder;

    QuotesGenerator(QDTicker ticker) {
        qdDistributor = ticker.distributorBuilder().build();
        subscriptionHolder = new SubscriptionHolder(qdDistributor, x -> {});
    }

    void publishQuotes() {
//        Set<String> symbols = generateSymbols();

        Random random = new Random();
        while (true) {
            try {
                RecordBuffer buffer = RecordBuffer.getInstance();
                for (String symbol : subscriptionHolder.getSubscription()) {
//                for (String symbol : symbols) {
                    RecordCursor cur = buffer.add(QUOTE, CODEC.encode(symbol), symbol);
                    double price = 100 * random.nextDouble();
                    cur.setLong(BID_PRICE_INDEX, WideDecimal.composeWide(price));
                }

                qdDistributor.process(buffer);
                calcIterations.incrementAndGet();
                counter.addAndGet(buffer.size());

                buffer.release();

                Thread.sleep(5);
            } catch (Exception e) {
                System.out.println(e);
                e.printStackTrace();
            }
        }
    }

    private void printStat() {
        System.out.println("subscriptions: " + subscriptionHolder.size());
        System.out.println("calcIterations: " + calcIterations.getAndSet(0) / REPORT_PERIOD);
        System.out.println("counter: " + counter.getAndSet(0) / REPORT_PERIOD);
    }
}
