package app;

import com.devexperts.logging.Logging;
import com.devexperts.qd.QDAgent;
import com.devexperts.qd.QDFactory;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.SymbolCodec;
import com.devexperts.qd.kit.DefaultRecord;
import com.devexperts.qd.kit.DefaultScheme;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordMode;
import com.devexperts.qd.qtp.AgentAdapter;
import com.devexperts.qd.qtp.DistributorAdapter;
import com.devexperts.qd.qtp.MessageAdapter;
import com.devexperts.qd.qtp.MessageConnectors;
import com.devexperts.qd.stats.QDStats;
import com.dxfeed.api.impl.DXFeedScheme;

import java.util.Set;

public class Util {

    public static final DefaultScheme scheme = DXFeedScheme.getInstance();
    public static final SymbolCodec CODEC = scheme.getCodec();
    public static final DefaultRecord QUOTE_RECORD = Util.scheme.findRecordByName("Quote");
    public static final int BID_PRICE_INDEX = QUOTE_RECORD.findFieldByName("Quote.Bid.Price").getIndex();

    public static final DefaultRecord GREEK_RECORD = Util.scheme.findRecordByName("Greeks");
    public static final int VOLATILITY_INDEX = GREEK_RECORD.findFieldByName("Greeks.Volatility").getIndex();

    public static QDTicker createTicker(QDStats stats) {
        return QDFactory.getDefaultFactory()
                .tickerBuilder()
                .withScheme(scheme)
                .withStats(stats)
                .build();
    }

    public static void startConsumerConnector(QDTicker ticker, String address) {
        MessageAdapter.Factory adapter =
                new DistributorAdapter.Factory(ticker, null, null, null);

        startConnector(adapter, address);
    }

    public static void startProducerConnector(QDTicker ticker, String address) {
        MessageAdapter.Factory adapter =
                new AgentAdapter.Factory(ticker, null, null, null);

        startConnector(adapter, address);
    }

    private static void startConnector(MessageAdapter.Factory adapter, String address) {
        MessageConnectors.startMessageConnectors(
                MessageConnectors.createMessageConnectors(
                        MessageConnectors.applicationConnectionFactory(adapter),
                        address)
        );
    }

    public static void setSubscription(QDAgent agent, DefaultRecord record, Set<String> symbols) {
        RecordBuffer buffer = RecordBuffer.getInstance(RecordMode.SUBSCRIPTION);
        for (String symbol : symbols) {
            buffer.add(record, scheme.getCodec().encode(symbol), symbol);
        }
        agent.setSubscription(buffer);
        buffer.release();
    }

    public static void initLog() {
        String logFile = System.getProperty("log");
        if (logFile != null) {
            Logging.configureLogFile(logFile);
        }
    }

    public static double calcVolatility(double bidPrice, int complexity) {
        // A simple computation to simulate CPU work
        long result = 0;

        for (int i = 1; i <= complexity; i++) {
            result += Math.pow(bidPrice, 2) * Math.sqrt(i);
        }
        return result;
    }
}
