package com.devexperts.qd.tools;

import com.devexperts.logging.Logging;
import com.devexperts.qd.*;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordProvider;
import com.devexperts.qd.qtp.*;
import com.devexperts.qd.stats.QDStats;


public class VkNetTestProducerAdapter extends MessageAdapter {
    private static final Logging log = Logging.getLogging(VkNetTestProducerAdapter.class);

    public static class Factory extends ConfigurableFactory {
        private final RecordProvider recordProvider;
        private final long publishingPeriod;
        private final Stat stat;

        Factory(RecordProvider recordProvider, long publishingPeriod, Stat stat) {
//            super(ticker, null, null, null);
            this.recordProvider = recordProvider;
            this.publishingPeriod = publishingPeriod;
            this.stat = stat;
        }

        @Override
        public MessageAdapter createAdapter(QDStats stats) {
            return new VkNetTestProducerAdapter(stats, stat, recordProvider, publishingPeriod, QDFilter.ANYTHING);
        }
    }

    private final Stat stat;
    private final RecordProvider recordProvider;
    // kind of data generation trigger
    private volatile boolean receivedProtocolDescriptor;
    private final QDFilter filter;
    private final long publishingPeriod;


    VkNetTestProducerAdapter(QDStats stats, Stat stat, RecordProvider recordProvider, long publishingPeriod, QDFilter filter) {
        super(null, stats);
        this.doNotCloseOnErrors = true; // special mode to decode even bad stuff

        this.filter = filter;
        this.recordProvider = recordProvider;
        this.publishingPeriod = publishingPeriod;
        this.stat = stat;
        useDescribeProtocol();
        addMask(getMessageMask(VkNetTest.SUBSCRIPTION_TYPE));
        offerData();

//        if (publishingPeriod > 0) {
//            Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(
//                    this::offerData, 0, publishingPeriod, TimeUnit.MILLISECONDS);
//        }
    }

    // notifies TransportConnection that there is data to send.
    private void offerData() {
        addMask(getMessageMask(VkNetTest.DATA_TYPE));
    }

    @Override
    public DataScheme getScheme() {
        return VkNetTest.SCHEME;
    }


    @Override
    public void prepareProtocolDescriptor(ProtocolDescriptor desc) {
        super.prepareProtocolDescriptor(desc);
        QDFilter stableFilter = filter.toStableFilter();
        if (stableFilter != QDFilter.ANYTHING)
            desc.setProperty(ProtocolDescriptor.FILTER_PROPERTY, stableFilter.toString());

        // will send all kinds of data messages
        for (MessageType type : MessageType.values())
            if (type.isData())
                desc.addSend(desc.newMessageDescriptor(type));
    }

    @Override
    public boolean isProtocolDescriptorCompatible(ProtocolDescriptor desc) {
        // is compatible is can receive any kind of data message
        for (MessageType type : MessageType.values())
            if (type.isData() && desc.canReceive(type))
                return true;
        return false;
    }

    @Override
    public void processDescribeProtocol(ProtocolDescriptor desc, boolean logDescriptor) {
        super.processDescribeProtocol(desc, logDescriptor);
        receivedProtocolDescriptor = true;
//        if (publishingPeriod <= 0) {
//            offerData();
//        }
        notifyListener();
    }

    @Override
    protected void processSubscription(SubscriptionIterator iterator, MessageType message) {
        // Just in case we have a legacy QD on the other side that sends us subscription anyway
        int c = 0;
        while (iterator.nextRecord() != null)
            c++;
        log.warn("Ignored " + c + " " + message + " messages");
    }

    @Override
    public boolean retrieveMessages(MessageVisitor visitor) {
        super.retrieveMessages(visitor);
        long mask = retrieveMask();
        if (hasMessageMask(mask, MessageType.DESCRIBE_PROTOCOL))
            mask = retrieveDescribeProtocolMessage(visitor, mask);
        addMask(mask);
        if (!receivedProtocolDescriptor) {
            // wait until consumer send its ProtocolDescriptor
            return false;
        }

        generateData(visitor);
        return true;
    }

    private void generateData(MessageVisitor visitor) {
        RecordBuffer buffer = RecordBuffer.getInstance();
        recordProvider.retrieve(buffer);
        stat.counter.addAndGet(buffer.size());
        visitor.visitData(buffer, VkNetTest.DATA_TYPE);
        buffer.release();

//        System.out.println(stat.counter.get());
    }
}
