package app.v1;

import misc.SubscriptionCollector;
import app.Util;
import com.devexperts.logging.Logging;
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

// FIXME: it's analogue of "./qds nettest -Dcom.devexperts.qd.tools.NetTest.record=Quote ..."
public class QuotesProducer {
    private static final Logging log = Logging.getLogging(QuotesProducer.class);

    private static final int STAT_REPORT_PERIOD = 10;
    private static final AtomicLong calcIterations = new AtomicLong();
    private static final AtomicLong recordCounter = new AtomicLong();

    public static void main(String[] args) throws InterruptedException {
        initLog();

        QDTicker ticker = Util.createTicker(QDStats.VOID);
        String address = args[0];
        Util.startProducerConnector(ticker, address);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        QuotesProducer quotesProducer = new QuotesProducer(ticker);
        executorService.scheduleWithFixedDelay(
                quotesProducer::printStat, 0, STAT_REPORT_PERIOD, TimeUnit.SECONDS);

        quotesProducer.publishQuotes();

        Thread.sleep(Long.MAX_VALUE);
    }

    private final QDDistributor qdDistributor;
    private final SubscriptionCollector subscriptionCollector;

    public QuotesProducer(QDTicker ticker) {
        qdDistributor = ticker.distributorBuilder().build();
        subscriptionCollector = new SubscriptionCollector(qdDistributor);
    }

    private void publishQuotes() {
//        Set<String> symbols = generateSymbols();

        Random random = new Random();
        while (true) {
            try {
                RecordBuffer buffer = RecordBuffer.getInstance();
                for (String symbol : subscriptionCollector.getSubscription(QUOTE_RECORD.getName())) {
//                for (String symbol : symbols) {
                    RecordCursor cur = buffer.add(QUOTE_RECORD, CODEC.encode(symbol), symbol);
                    double price = 100 * random.nextDouble();
                    cur.setLong(BID_PRICE_INDEX, WideDecimal.composeWide(price));
                }

                qdDistributor.process(buffer);
                calcIterations.incrementAndGet();
                recordCounter.addAndGet(buffer.size());

                buffer.release();

                Thread.sleep(5);
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
    }

    private void printStat() {
        log.info("subscriptions: " + subscriptionCollector.getSubscription(QUOTE_RECORD.getName()).size());
        log.info("calcIterations: " + calcIterations.getAndSet(0) / STAT_REPORT_PERIOD);
        log.info("RPS: " + recordCounter.getAndSet(0) / STAT_REPORT_PERIOD);
    }
}
