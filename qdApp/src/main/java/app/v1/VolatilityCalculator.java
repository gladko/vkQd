package app.v1;

import misc.SubscriptionCollector;
import app.Util;
import com.devexperts.logging.Logging;
import com.devexperts.qd.QDAgent;
import com.devexperts.qd.QDDistributor;
import com.devexperts.qd.QDFilter;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.kit.HashFilter;
import com.devexperts.qd.ng.AbstractRecordSink;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordCursor;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.util.WideDecimal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static app.Util.*;

public class VolatilityCalculator {
    private static final Logging log = Logging.getLogging(VolatilityCalculator.class);

    private static final int STAT_REPORT_PERIOD = 10;
    private static final int VOLATILITY_CALC_COMPLEXITY = 300; // must be between 100 and 300
    private static final int WORKERS_COUNT = 1;
    private static final List<Worker> workers = new ArrayList<>();
    private static final AtomicLong calcIterationsCounter = new AtomicLong();
    private static final AtomicLong producerRecordsCounter = new AtomicLong();

    public static void main(String[] args) throws InterruptedException {
        initLog();

        QDTicker ticker = Util.createTicker(QDStats.VOID);
        String agentAddress = args[0];       // for consuming Quotes
        String distributorAddress = args[1]; // for distributing Greeks
        initConnectors(ticker, agentAddress, distributorAddress);

        for (int i = 0; i < WORKERS_COUNT; i++) {
            Worker worker = new Worker(ticker, i, WORKERS_COUNT);
            workers.add(worker);
            worker.start();
        }

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(
                VolatilityCalculator::printStat, 0, STAT_REPORT_PERIOD, TimeUnit.SECONDS);

        Thread.sleep(Long.MAX_VALUE);
    }

    // FIXME: connection filters are needed
    private static void initConnectors(QDTicker ticker, String agentAddress, String distributorAddress) {
        Util.startConsumerConnector(ticker, agentAddress);
        Util.startProducerConnector(ticker, distributorAddress);
    }

    static class Worker extends Thread {
        QDDistributor qdDistributor;
        QDAgent agent;
        SubscriptionCollector subscription;

        Worker(QDTicker ticker, int index, int workers) {
            QDFilter filter = workers > 1 ? HashFilter.valueOf(scheme, HashFilter.formatName(index, workers))
                : QDFilter.ANYTHING;

            qdDistributor = ticker.distributorBuilder()
                    .withFilter(filter)
                    .build();
            agent = ticker.agentBuilder().build();

            subscription = new SubscriptionCollector(qdDistributor,
                    s -> Util.setSubscription(agent, Util.QUOTE_RECORD, s.get(QUOTE_RECORD.getName())));
        }

        @Override
        public void run() {
            while (true) {
                try {
                    RecordBuffer inBuffer = RecordBuffer.getInstance();
                    RecordBuffer outBuffer = RecordBuffer.getInstance();
                    agent.retrieve(inBuffer);
                    inBuffer.retrieve(new AbstractRecordSink() {
                        @Override
                        public void append(RecordCursor cursor) {
                            double bidPrice = WideDecimal.toDouble(cursor.getLong(BID_PRICE_INDEX));
                            RecordCursor outCursor = outBuffer.add(GREEK_RECORD, cursor.getCipher(), cursor.getSymbol());
                            double volatility = calcVolatility(bidPrice, VOLATILITY_CALC_COMPLEXITY);
                            outCursor.setLong(VOLATILITY_INDEX, WideDecimal.composeWide(volatility));
                        }
                    });
                    if (!outBuffer.isEmpty()) {
                        qdDistributor.process(outBuffer);
                        calcIterationsCounter.incrementAndGet();
                        producerRecordsCounter.addAndGet(outBuffer.size());
                    }
                    inBuffer.release();
                    outBuffer.release();
                    Thread.sleep(5);
                } catch (Exception e) {
                    log.error(e.toString(), e);
                }
            }
        }
    }

    private static void printStat() {
        AtomicInteger totalSubscription = new AtomicInteger();
        workers.forEach(w -> {
            totalSubscription.addAndGet(w.subscription.getSubscription(GREEK_RECORD.getName()).size());
//            System.out.println("subscription: " + w.subscription.size());
        });
        log.info("totalSubscription: " + totalSubscription);
        log.info("calcIterations: " + calcIterationsCounter.getAndSet(0) / STAT_REPORT_PERIOD);
        log.info("RPS: " + producerRecordsCounter.getAndSet(0) / STAT_REPORT_PERIOD);
    }
}
