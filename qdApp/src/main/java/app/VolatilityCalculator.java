package app;

import com.devexperts.qd.QDAgent;
import com.devexperts.qd.QDDistributor;
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
    private static final int ITERATIONS = 300; // must be between 100 and 300
    private static final int WORKERS_COUNT = 1;
    private static final List<Worker> workers = new ArrayList<>();
    private static final AtomicLong calcIterations = new AtomicLong();
    private static final AtomicLong counter = new AtomicLong();

    public static void main(String[] args) throws InterruptedException {
        QDTicker ticker = Util.createTicker(QDStats.VOID);

        initConnectors(ticker);

        for (int i = 0; i < WORKERS_COUNT; i++) {
            Worker worker = new Worker(ticker, i, WORKERS_COUNT);
            workers.add(worker);
            worker.start();
        }

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(
                VolatilityCalculator::printStat, 0, REPORT_PERIOD, TimeUnit.SECONDS);

        Thread.sleep(Long.MAX_VALUE);
    }

    private static void initConnectors(QDTicker ticker) {
        Util.startAgentConnector(ticker, ":7000"); // for distributing Greeks
        Util.startDistributorConnector(ticker, "127.0.0.1:9000"); // for consuming Quotes
    }

    static class Worker extends Thread {
        QDDistributor qdDistributor;
        QDAgent agent;
        SubscriptionHolder subscription;

        Worker(QDTicker ticker, int index, int workers) {
            qdDistributor = ticker.distributorBuilder()
                    .withFilter(HashFilter.valueOf(scheme, HashFilter.formatName(index, workers)))
                    .build();
            agent = ticker.agentBuilder().build();

            subscription = new SubscriptionHolder(qdDistributor, s -> Util.setSubscription(agent, Util.QUOTE, s));
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
                            RecordCursor outCursor = outBuffer.add(GREEK, cursor.getCipher(), cursor.getSymbol());
                            outCursor.setLong(VOLATILITY_INDEX, WideDecimal.composeWide(calcVolatility(bidPrice)));
                        }
                    });
                    if (!outBuffer.isEmpty()) {
                        qdDistributor.process(outBuffer);
                        calcIterations.incrementAndGet();
                        counter.addAndGet(outBuffer.size());
                    }
                    inBuffer.release();
                    outBuffer.release();
                } catch (Exception e) {
                    System.out.println(e);
                    e.printStackTrace();
                }
            }
        }

        private double calcVolatility(double bidPrice) {
            // A simple computation to simulate CPU work
            long result = 0;

            for (int i = 1; i <= ITERATIONS; i++) {
                result += Math.pow(bidPrice, 2) * Math.sqrt(i);
            }
            return result;
        }
    }

    private static void printStat() {
        AtomicInteger totalSubscr = new AtomicInteger();
        workers.forEach(w -> {
            totalSubscr.addAndGet(w.subscription.size());
//            System.out.println("subscription: " + w.subscription.size());
        });
        System.out.println("totalSubscription: " + totalSubscr);
        System.out.println("calcIterations: " + calcIterations.getAndSet(0) / REPORT_PERIOD);
        System.out.println("counter: " + counter.getAndSet(0) / REPORT_PERIOD);
    }
}
