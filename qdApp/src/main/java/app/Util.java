package app;

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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Util {
    static final int REPORT_PERIOD = 10;
    static final DefaultScheme scheme = DXFeedScheme.getInstance();
    static final SymbolCodec CODEC = scheme.getCodec();
    static final DefaultRecord QUOTE = Util.scheme.findRecordByName("Quote");
    static final int BID_PRICE_INDEX = QUOTE.findFieldByName("Quote.Bid.Price").getIndex();

    static final DefaultRecord GREEK = Util.scheme.findRecordByName("Greeks");
    static final int VOLATILITY_INDEX = GREEK.findFieldByName("Greeks.Volatility").getIndex();

    public static QDTicker createTicker(QDStats stats) {
        return QDFactory.getDefaultFactory()
                .tickerBuilder()
                .withScheme(scheme)
                .withStats(stats)
                .build();
    }

    public static void startAgentConnector(QDTicker ticker, String address) {
        MessageAdapter.Factory adapter =
                new AgentAdapter.Factory(ticker, null, null, null);

        startConnector(adapter, address);
    }

    public static void startDistributorConnector(QDTicker ticker, String address) {
        MessageAdapter.Factory adapter =
                new DistributorAdapter.Factory(ticker, null, null, null);

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

    public static Set<String> generateSymbols() {
        Set<String> symbols = new HashSet<>();
        byte[] tmp = new byte[10];
        Random random = new Random();
        do {
            random.nextBytes(tmp);
            symbols.add(new String(tmp));
        } while (symbols.size() < 100_000);
        return symbols;
    }
}
