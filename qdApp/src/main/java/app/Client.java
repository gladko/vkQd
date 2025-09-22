package app;

import com.devexperts.qd.QDAgent;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordMode;
import com.devexperts.qd.ng.RecordProvider;
import com.devexperts.qd.stats.QDStats;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static app.Util.*;


public class Client {
    static final int CONSUMERS_COUNT = 20;
    private static final AtomicLong counter = new AtomicLong();

    public static void main(String[] args) throws InterruptedException {
        Set<String> symbols = generateSymbols();

        QDTicker ticker = Util.createTicker(QDStats.VOID);
        startDistributorConnector(ticker, "127.0.0.1:7000");

        for (int i = 0; i < CONSUMERS_COUNT; i++) {
            new Consumer(ticker, symbols, i);
        }

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(
                Client::printStat, 0, REPORT_PERIOD, TimeUnit.SECONDS);

        Thread.sleep(Long.MAX_VALUE);
    }

    static class Consumer {
        final int id;
        final QDAgent qdAgent;

        Consumer(QDTicker ticker, Set<String> symbols, int id) {
            this.id = id;
            qdAgent = ticker.agentBuilder().build();
            qdAgent.setRecordListener(this::onDataAvailable);
            Util.setSubscription(qdAgent, GREEK, symbols);
        }

        private void onDataAvailable(RecordProvider recordProvider) {
            RecordBuffer buffer = RecordBuffer.getInstance(RecordMode.DATA);
            recordProvider.retrieve(buffer);
//            System.out.println("client " + id + " received " + buffer.size());
            counter.addAndGet(buffer.size());
            buffer.release();
        }
    }


    private static void printStat() {
        System.out.println("counter: " + counter.getAndSet(0) / CONSUMERS_COUNT / REPORT_PERIOD);
    }
}
