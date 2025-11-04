package app.v1;

import app.Util;
import com.devexperts.logging.Logging;
import com.devexperts.qd.QDAgent;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordMode;
import com.devexperts.qd.ng.RecordProvider;
import com.devexperts.qd.stats.QDStats;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static app.Util.*;

// FIXME: it's analogue of "./qds nettest -Dcom.devexperts.qd.tools.NetTest.record=Greeks ..."
public class Client {
    private static final Logging log = Logging.getLogging(Client.class);

    static final int SUBSCRIPTION_SYMBOLS_COUNT = 100_000;
    static final int STAT_REPORT_PERIOD = 10;
    static final int CONSUMERS_COUNT = 1;
    private static final AtomicLong recordCounter = new AtomicLong();

    public static void main(String[] args) throws InterruptedException {
        initLog();
        Set<String> symbols = generateSymbols(SUBSCRIPTION_SYMBOLS_COUNT);

        QDTicker ticker = Util.createTicker(QDStats.VOID);
        String address = args[0];
        startConsumerConnector(ticker, address);

        for (int i = 0; i < CONSUMERS_COUNT; i++) {
            new Consumer(ticker, symbols, i);
        }

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(
                Client::printStat, 0, STAT_REPORT_PERIOD, TimeUnit.SECONDS);

        Thread.sleep(Long.MAX_VALUE);
    }

    static class Consumer {
        final int id;
        final QDAgent qdAgent;

        Consumer(QDTicker ticker, Set<String> symbols, int id) {
            this.id = id;
            qdAgent = ticker.agentBuilder().build();
            qdAgent.setRecordListener(this::onDataAvailable);
            Util.setSubscription(qdAgent, GREEK_RECORD, symbols);
        }

        private void onDataAvailable(RecordProvider recordProvider) {
            RecordBuffer buffer = RecordBuffer.getInstance(RecordMode.DATA);
            recordProvider.retrieve(buffer);
//            System.out.println("client " + id + " received " + buffer.size());
            recordCounter.addAndGet(buffer.size());
            buffer.release();
        }
    }


    private static void printStat() {
        log.info("RPS: " + recordCounter.getAndSet(0) / CONSUMERS_COUNT / STAT_REPORT_PERIOD);
    }

    private static Set<String> generateSymbols(int count) {
        Set<String> symbols = new HashSet<>();
        byte[] tmp = new byte[10];
        Random random = new Random();
        do {
            random.nextBytes(tmp);
            symbols.add(new String(tmp));
        } while (symbols.size() < count);
        return symbols;
    }
}
