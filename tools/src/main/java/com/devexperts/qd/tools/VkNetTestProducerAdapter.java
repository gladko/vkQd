package com.devexperts.qd.tools;

import com.devexperts.logging.Logging;
import com.devexperts.qd.DataScheme;
import com.devexperts.qd.QDFilter;
import com.devexperts.qd.SubscriptionIterator;
import com.devexperts.qd.ng.RecordProvider;
import com.devexperts.qd.qtp.MessageAdapter;
import com.devexperts.qd.qtp.MessageType;
import com.devexperts.qd.qtp.MessageVisitor;
import com.devexperts.qd.qtp.ProtocolDescriptor;
import com.devexperts.qd.stats.QDStats;


public class VkNetTestProducerAdapter extends MessageAdapter {
    private static final Logging log = Logging.getLogging(VkNetTestProducerAdapter.class);

    public static class Factory extends ConfigurableFactory {
        private final RecordProvider dataGenerator;
        private final Stat stat;

        Factory(RecordProvider dataGenerator, Stat stat) {
            this.dataGenerator = dataGenerator;
            this.stat = stat;
        }

        @Override
        public MessageAdapter createAdapter(QDStats stats) {
            return new VkNetTestProducerAdapter(stats, stat, dataGenerator, QDFilter.ANYTHING);
        }
    }


    private final RecordProvider dataGenerator;
    // kind of data generation trigger
    private volatile boolean receivedProtocolDescriptor;
    private final QDFilter filter;


    VkNetTestProducerAdapter(QDStats stats, Stat stat, RecordProvider dataGenerator, QDFilter filter) {
        super(null, stats);
        this.doNotCloseOnErrors = true; // special mode to decode even bad stuff

        this.filter = filter;
        this.dataGenerator = dataGenerator;
        useDescribeProtocol();
        addMask(getMessageMask(VkNetTest.SUBSCRIPTION_TYPE));
        offerData();
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
        super.processDescribeProtocol(desc, true);
        receivedProtocolDescriptor = true;
        notifyListener();
    }

    @Override
    protected void processSubscription(SubscriptionIterator iterator, MessageType message) {
        // Just in case we have a legacy QD on the other side that sends us subscription anyway
        int c = 0;
        while (iterator.nextRecord() != null)
            c++;
        log.debug("Ignored " + c + " " + message + " messages");
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

        visitor.visitData(dataGenerator, VkNetTest.DATA_TYPE);

        // returning false to finish current iteration and send data but
        // at the same time immediately notify that new data is already available and may be retrieved
        addMask(getMessageMask(VkNetTest.DATA_TYPE));
        return false;
    }
}
