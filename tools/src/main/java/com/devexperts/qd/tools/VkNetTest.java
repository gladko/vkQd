package com.devexperts.qd.tools;

import com.devexperts.qd.*;
import com.devexperts.qd.kit.DefaultScheme;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordMode;
import com.devexperts.qd.qtp.MessageAdapter;
import com.devexperts.qd.qtp.MessageConnector;
import com.devexperts.qd.qtp.MessageConnectors;
import com.devexperts.qd.qtp.MessageType;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.services.ServiceProvider;
import com.dxfeed.api.impl.DXFeedScheme;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Simplified version of QDS NetTest tool. This tool should consume much less CPU than the original version.
 * It does not create QdCollector(s) and only pushes / pulls data directly to / from java socket.
 *
 * @see NetTestSide
 */
@ToolSummary(
        info = "Tests network throughput.",
        argString = "<side> <address>",
        arguments = {
                "<side>    -- either 'p' (producer) or 'c' (consumer)",
                "<address> -- address to connect (see @link{address})"
        }
)
@ServiceProvider
public class VkNetTest extends AbstractTool {
    private static final int STAT_PERIOD = 10;
    private static final int PUBLISHING_PERIOD = 5000;
    private static final int RECORDS_PER_ITERATION = 1000;
    static final MessageType SUBSCRIPTION_TYPE = MessageType.TICKER_ADD_SUBSCRIPTION;
    static final MessageType DATA_TYPE = MessageType.TICKER_DATA;

    static final DefaultScheme SCHEME = DXFeedScheme.getInstance();

    private final NetTest.OptionSymbols symbolsOption = new NetTest.OptionSymbols();
    private final OptionInteger connectionsOption = new OptionInteger('C', "connections", "<number>", "Number of instances to create.");
    private final OptionLog logfileOption = OptionLog.getInstance();
    private final OptionString recordNameOption = new OptionString('R', "record", "<name>", "Record name");

    protected static DataRecord record;
    private final Stat stat = new Stat();
    private int connectionsCount;

    @Override
    protected Option[] getOptions() {
        return new Option[] {logfileOption, symbolsOption, connectionsOption};
    }

    @Override
    protected void executeImpl(String[] args) {
        if (args.length == 0) {
            noArguments();
        }
        if (args.length != 2) {
            wrongNumberOfArguments();
        }

        String recordName = recordNameOption.isSet() ? recordNameOption.getValue() : "TimeAndSale";
        record = SCHEME.findRecordByName(recordName);
        log.info("Using record " + record);

        String address = args[1];
//        QDTicker ticker = createTicker(stat.rootStat);
        int symbolsCount = symbolsOption.isSet() ? symbolsOption.getTotal() : 100000;
        List<String> symbols = SymbolGenerator.generateSymbols(symbolsCount);
        connectionsCount = connectionsOption.isSet() ? connectionsOption.getValue() : 1;

        MessageAdapter.Factory maFactory;
        if (args[0].equalsIgnoreCase("p")) { // publisher
            SymbolList symbolList = new SymbolList(symbols.toArray(new String[0]), SCHEME.getCodec());
            RandomRecordsProvider provider = new RandomRecordsProvider(new DataRecord[] {record},
                    symbolList, RECORDS_PER_ITERATION, RECORDS_PER_ITERATION);

            maFactory = new VkNetTestProducerAdapter.Factory(provider, PUBLISHING_PERIOD, stat);
        } else if (args[0].equalsIgnoreCase("c")) { // consumer
            maFactory = new VkNetTestConsumerAdapter.Factory(buildSubscription(symbols), QDFilter.ANYTHING, stat);
        } else {
            throw new BadToolParametersException("<side> must be either 'p' (producer) or 'c' (consumer)");
        }

        for (int i = 0; i < connectionsCount; i++) {
            List<MessageConnector> connectors = MessageConnectors.createMessageConnectors(
                    MessageConnectors.applicationConnectionFactory(maFactory),
                    address, stat.rootStat);

            stat.connectorsStat.addConnectors(connectors);
            MessageConnectors.startMessageConnectors(connectors);
        }

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
                this::printStat, 0, STAT_PERIOD, TimeUnit.SECONDS);
    }

    private void printStat() {
        System.out.println("\n" + stat.diff() / STAT_PERIOD + " RPS (" + connectionsCount + " connections)");
        System.out.println(stat.connectorsStat.report());
    }

    private RecordBuffer buildSubscription(List<String> symbols) {
        RecordBuffer buffer = new RecordBuffer(RecordMode.SUBSCRIPTION);
        for (String symbol : symbols) {
            buffer.add(record, SCHEME.getCodec().encode(symbol), symbol);
        }
        return buffer;
    }

//    public static QDTicker createTicker(QDStats stats) {
//        return QDFactory.getDefaultFactory()
//                .tickerBuilder()
//                .withScheme(scheme)
//                .withStats(stats)
//                .build();
//    }


    @Override
    public Thread mustWaitForThread() {
        return Thread.currentThread();
    }

    public static void main(String[] args) {
        Tools.executeSingleTool(VkNetTest.class, args);
    }
}
