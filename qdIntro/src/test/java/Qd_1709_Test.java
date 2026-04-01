import com.devexperts.qd.*;
import com.devexperts.qd.kit.AbstractDataIntField;
import com.devexperts.qd.kit.DefaultRecord;
import com.devexperts.qd.kit.DefaultScheme;
import com.devexperts.qd.monitoring.ConnectorsMonitoringTask;
import com.devexperts.qd.monitoring.MonitoringEndpoint;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordCursor;
import com.devexperts.qd.ng.RecordMode;
import com.devexperts.qd.ng.RecordProvider;
import com.devexperts.qd.qtp.*;
import com.devexperts.qd.stats.QDStats;
import com.dxfeed.api.impl.DXFeedScheme;
import misc.SubscriptionCollector;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static com.devexperts.qd.qtp.MessageConnectors.*;
/**
 * Test reproduces bug in QD monitoring: QD-1709
 */
public class Qd_1709_Test {
    static final boolean MUX_CASE = false;
    static final boolean USE_QDENDPOINT = false;
    static final Collection<String> symbols = IntStream.range(0, 10)
            .mapToObj(Integer::toString)
            .toList();
    static final Collection<String> records = List.of("Quote", "Trade", "Greeks", "TheoPrice");
    static final DefaultScheme SCHEME = DXFeedScheme.getInstance();
    static final int MONITORING_PERIOD = 3;

    static ClientProducer producer;
    static ClientConsumer consumer;


    public static void main(String[] args) throws InterruptedException {
        QDTicker qdTicker1 = createTicker(QDStats.VOID);
        QDTicker qdTicker2 = createTicker(QDStats.VOID);
        // Opens data provider server socket
        startMessageConnectors(
                createMessageConnectors(
                        applicationConnectionFactory(
                new AgentAdapter.Factory(qdTicker1, null, null, null)),
                        ":123"));

        // Opens data consumer client socket
        startMessageConnectors(
                createMessageConnectors(
                        applicationConnectionFactory(
                new DistributorAdapter.Factory(qdTicker2, null, null, null)),
                        "localhost:123"));


        if (MUX_CASE) {
            // start 2 MUXes:
            //   ./qds multiplexor --stat 10s --log replication-mux-core.log "(:7001)(:7002)" :5000
            //   ./qds multiplexor --stat 10s --log replication-mux-quote.log :8000 :6000
            //
//            initWithoutQdEndpoint(args[0], args[1]);
            initWithoutQdEndpoint(
                    "(localhost:7001[name=push-core1])(localhost:7002[name=push-core1])(localhost:8000[name=push-quote])",
                    "(:Greeks,:TheoPrice@localhost:5000[name=pull-core])(:Quote,:Trade@localhost:6000[name=pull-quote])");

            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                System.out.println("Producer subscription: " + producer.subscriptions.getSubscription());
            }, 0, MONITORING_PERIOD, TimeUnit.SECONDS);
        } else {
            // connection top record case
            if (USE_QDENDPOINT) {
                initWithQdEndpoint(":9999", "localhost:9999");
            } else {
                initWithoutQdEndpoint(":9999", "localhost:9999");
            }
        }

        consumer.subscribe();
        new Thread(producer).start();

        Thread.sleep(Long.MAX_VALUE);
    }

    private static void initWithoutQdEndpoint(String producerAddress, String consumerAddress) {
        System.out.println("producerAddress: " + producerAddress);
        System.out.println("consumerAddress: " + consumerAddress);

        producer = new ClientProducer();
        connectProducer(producer, producerAddress);
        scheduleReporting(producer.stat.connectorsStat);

        consumer = new ClientConsumer();
        connectConsumer(consumer, consumerAddress);
        scheduleReporting(consumer.stat.connectorsStat);
    }

    private static void initWithQdEndpoint(String producerAddress, String consumerAddress) {
        System.out.println("producerAddress: " + producerAddress);
        System.out.println("consumerAddress: " + consumerAddress);

        QDEndpoint producerEndpoint = createQdEndpoint("producer");
        producer = new ClientProducer(producerEndpoint.getTicker());
        connectProducerEndpoint(producerEndpoint, producerAddress);

        QDEndpoint consumerEndpoint = createQdEndpoint("consumer");
        consumer = new ClientConsumer(consumerEndpoint.getTicker());
        connectConsumerEndpoint(consumerEndpoint, consumerAddress);
    }


    static class ClientConsumer {
        private final QDTicker ticker;
        private final QDAgent qdAgent;
        private final Stat stat;

        public ClientConsumer() {
            stat = new Stat(SCHEME);
            ticker = createTicker(stat.rootStat);

            qdAgent = ticker.agentBuilder().build();
            qdAgent.setRecordListener(this::onData);
        }

        public ClientConsumer(QDTicker ticker) {
            stat = null;
            this.ticker = null;
            qdAgent = ticker.agentBuilder().build();
            qdAgent.setRecordListener(this::onData);
        }

        private void onData(RecordProvider recordProvider) {
            RecordBuffer buffer = RecordBuffer.getInstance();
            recordProvider.retrieve(buffer);
//            System.out.println("retrieved " + buffer.size());

//            buffer.retrieve(new AbstractRecordSink() {
//                @Override
//                public void append(RecordCursor cursor) {
//                    System.out.println(cursor);
//                }
//            });
            buffer.release();
        }

        public void subscribe() {
            RecordBuffer buffer = RecordBuffer.getInstance(RecordMode.SUBSCRIPTION);
            for (String recordName : records) {
                DefaultRecord record = SCHEME.findRecordByName(recordName);
                for (String symbol : symbols) {
                    buffer.add(record, SCHEME.getCodec().encode(symbol), symbol);
                }
            }

            qdAgent.setSubscription(buffer);
            buffer.release();
        }
    }

    static class ClientProducer implements Runnable {
        private final Stat stat;
        private final QDTicker ticker;
        private final QDDistributor qdDistributor;
        private final SubscriptionCollector subscriptions;

        public ClientProducer() {
            stat = new Stat(SCHEME);
            ticker = createTicker(stat.rootStat);
            qdDistributor = ticker.distributorBuilder().build();
            subscriptions = new SubscriptionCollector(qdDistributor);
        }

        public ClientProducer(QDTicker ticker) {
            stat = null;
            this.ticker = null;
            qdDistributor = ticker.distributorBuilder().build();
            subscriptions = new SubscriptionCollector(qdDistributor);
        }

        @Override
        public void run() {
            while (true) {
                try {
                    RecordBuffer out = RecordBuffer.getInstance();
                    fill(out);
                    qdDistributor.process(out);
                    out.release();
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.out.println(e);
                }
            }
        }

        private void fill(RecordBuffer out) {
            for (String recordName : records) {
                for (String symbol : symbols) {
                    DefaultRecord record = SCHEME.findRecordByName(recordName);
                    RecordCursor cur = out.add(record, SCHEME.getCodec().encode(symbol), symbol);

                    for (int i = 0; i < record.getIntFieldCount(); i++) {
                        AbstractDataIntField intField = record.getIntField(i);
                        cur.setInt(intField.getIndex(), nextValue(intField));
                        if (intField.getSerialType().isLong()) {
                            i++;
                        }
                    }
                }
            }
        }

        private int nextValue(AbstractDataIntField intField) {
            return ThreadLocalRandom.current().nextInt(100);
        }
    }

    static class Stat {
        final QDStats rootStat;
        final ConnectorsMonitoringTask connectorsStat;

        Stat(DataScheme scheme) {
            rootStat = new QDStats(QDStats.SType.ANY, scheme);
            connectorsStat = new ConnectorsMonitoringTask(rootStat);
        }
    }


    ////////////////
    // UTIL
    ///////////////
    private static QDTicker createTicker(QDStats rootStat) {
        return QDFactory.getDefaultFactory()
                .tickerBuilder()
                .withScheme(SCHEME)
                .withStats(rootStat.create(QDStats.SType.TICKER))
                .build();
    }

    public static QDEndpoint createQdEndpoint(String name) {
        return QDEndpoint.newBuilder()
                .withCollectors(EnumSet.of(QDContract.TICKER))
                .withName(name)
                .withScheme(SCHEME)
                .withProperty(MonitoringEndpoint.MONITORING_STAT_PROPERTY, Integer.toString(MONITORING_PERIOD))
                .build();
    }

    private static void connectProducerEndpoint(QDEndpoint qdEndpoint, String address) {
        qdEndpoint.addConnectors(MessageConnectors.createMessageConnectors(
                MessageConnectors.applicationConnectionFactory(
                        new AgentAdapter.Factory(qdEndpoint, null)),
                address, qdEndpoint.getRootStats()));

        qdEndpoint.startConnectors();
    }

    private static void connectConsumerEndpoint(QDEndpoint qdEndpoint, String address) {
        qdEndpoint.addConnectors(MessageConnectors.createMessageConnectors(
                MessageConnectors.applicationConnectionFactory(
                        new DistributorAdapter.Factory(qdEndpoint, null)),
                address, qdEndpoint.getRootStats()));

        qdEndpoint.startConnectors();
    }

    private static void connectConsumer(ClientConsumer consumer, String address) {
        List<MessageConnector> connectors = connect(
                new DistributorAdapter.Factory(consumer.ticker, null, null, null),
                address, consumer.stat.rootStat);

        consumer.stat.connectorsStat.addConnectors(connectors);
    }

    private static void connectProducer(ClientProducer producer, String address) {
        List<MessageConnector> connectors = connect(
                new AgentAdapter.Factory(producer.ticker, null, null, null),
                address, producer.stat.rootStat);

        producer.stat.connectorsStat.addConnectors(connectors);
    }

    public static List<MessageConnector> connect(MessageAdapter.ConfigurableFactory factory, String address,
                                                 QDStats stat)
    {
        List<MessageConnector> connectors = MessageConnectors.createMessageConnectors(
                MessageConnectors.applicationConnectionFactory(factory), address, stat);
        MessageConnectors.startMessageConnectors(connectors);
        return connectors;
    }

    private static void scheduleReporting(ConnectorsMonitoringTask connectorsStat) {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(connectorsStat, 0,
                MONITORING_PERIOD, TimeUnit.SECONDS);
    }
}
