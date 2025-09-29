package com.devexperts.qd.tools;

import com.devexperts.qd.DataRecord;
import com.devexperts.qd.QDFilter;
import com.devexperts.qd.kit.DefaultScheme;
import com.devexperts.qd.qtp.*;
import com.devexperts.services.ServiceProvider;
import com.dxfeed.api.impl.DXFeedScheme;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private static final int MAX_RECORDS_PER_ITERATION = 1000;
    static final MessageType SUBSCRIPTION_TYPE = MessageType.TICKER_ADD_SUBSCRIPTION;
    static final MessageType DATA_TYPE = MessageType.TICKER_DATA;

    static final DefaultScheme SCHEME = DXFeedScheme.getInstance();

    private final OptionLog logfileOption = OptionLog.getInstance();
    private final NetTest.OptionSymbols symbolsOption = new NetTest.OptionSymbols();
    private final OptionInteger connectionsOption = new OptionInteger('C', "connections", "<number>", "Number of instances to create.");
    private final OptionString recordNameOption = new OptionString('R', "record", "<name>", "Record name");
    private final OptionInteger statPeriodOption = new OptionInteger('s', "stat", "<period>", "Period of statistics reporting");
    private final OptionEnum dataGenerationOption = new OptionEnum('g', "generation-mode", "Data generation mode", "random", "random", "seq");

    protected static DataRecord record;

    private int statPeriod;
    private final Stat stat = new Stat();
    private final List<MessageConnector> connectors = new CopyOnWriteArrayList<>();

    @Override
    protected Option[] getOptions() {
        return new Option[] {logfileOption, symbolsOption, connectionsOption, recordNameOption, statPeriodOption, dataGenerationOption};
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

        statPeriod = statPeriodOption.isSet() ? statPeriodOption.getValue() : 10;

        String address = args[1];
        int symbolsCount = symbolsOption.isSet() ? symbolsOption.getTotal() : 100000;
        List<String> symbols = SymbolGenerator.generateSymbols(symbolsCount);
        int connectionsCount = connectionsOption.isSet() ? connectionsOption.getValue() : 1;

        MessageAdapter.Factory maFactory;
        if (args[0].equalsIgnoreCase("p")) { // publisher
            TestDataGenerator.Mode mode = TestDataGenerator.Mode.valueOf(
                    dataGenerationOption.getValueOrDefault().toUpperCase());
            TestDataGenerator provider = new TestDataGenerator(List.of(record), symbols, mode,
                    MAX_RECORDS_PER_ITERATION, stat.counter);

            maFactory = new VkNetTestProducerAdapter.Factory(provider, stat);
        } else if (args[0].equalsIgnoreCase("c")) { // consumer
            maFactory = new VkNetTestConsumerAdapter.Factory(symbols, QDFilter.ANYTHING, stat);
        } else {
            throw new BadToolParametersException("<side> must be either 'p' (producer) or 'c' (consumer)");
        }

        for (int i = 0; i < connectionsCount; i++) {
            connectors.addAll(MessageConnectors.createMessageConnectors(
                    MessageConnectors.applicationConnectionFactory(maFactory),
                    address, stat.rootStat));
        }
        stat.connectorsStat.addConnectors(connectors);
        MessageConnectors.startMessageConnectors(connectors);

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
                this::printStat, 0, statPeriod, TimeUnit.SECONDS);
    }

    private void printStat() {
        int connectionsCount = connectors.stream()
                .mapToInt(MessageConnectorMBean::getConnectionCount)
                .sum();
        log.info("\n" + stat.diff() / statPeriod + " RPS (" + connectionsCount + " connections)");
        log.info(stat.connectorsStat.report());
    }


    @Override
    public Thread mustWaitForThread() {
        return Thread.currentThread();
    }

    public static void main(String[] args) {
        Tools.executeSingleTool(VkNetTest.class, args);
    }
}
