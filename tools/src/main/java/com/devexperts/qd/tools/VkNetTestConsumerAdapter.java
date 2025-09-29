package com.devexperts.qd.tools;

import com.devexperts.logging.Logging;
import com.devexperts.qd.*;
import com.devexperts.qd.ng.*;
import com.devexperts.qd.qtp.*;
import com.devexperts.qd.stats.QDStats;
import com.devexperts.qd.util.DataIterators;


public class VkNetTestConsumerAdapter extends MessageAdapter {
    private static final Logging log = Logging.getLogging(VkNetTestConsumerAdapter.class);

    public static class Factory extends MessageAdapter.ConfigurableFactory {
        private final RecordBuffer subscription;
        private final Stat stat;
        private final QDFilter filter;

        Factory(RecordBuffer subscription, QDFilter filter, Stat stat) {
//            super(ticker, null, null, filter);
            this.subscription = subscription;
            this.filter = filter;
            this.stat = stat;
        }

        @Override
        public MessageAdapter createAdapter(QDStats stats) {
            return new VkNetTestConsumerAdapter(stats, stat, subscription, filter);
        }
    }

    private final QDFilter filter;
    private final RecordBuffer subscription;
    private final Stat stat;

    VkNetTestConsumerAdapter(QDStats stats, Stat stat, RecordBuffer subscription, QDFilter filter) {
        super(null, stats);
        this.doNotCloseOnErrors = true; // special mode to decode even bad stuff
        this.subscription = new RecordBuffer();
        this.subscription.process(subscription);
        this.filter = filter;
        this.stat = stat;
        useDescribeProtocol();
        addMask(getMessageMask(VkNetTest.SUBSCRIPTION_TYPE));
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
        desc.addReceive(desc.newMessageDescriptor(MessageType.RAW_DATA));
        for (QDContract contract : QDContract.values()) {
            desc.addReceive(desc.newMessageDescriptor(MessageType.forData(contract)));
            desc.addReceive(desc.newMessageDescriptor(MessageType.forAddSubscription(contract)));
            desc.addReceive(desc.newMessageDescriptor(MessageType.forRemoveSubscription(contract)));
        }
    }

    @Override
    public synchronized void processData(DataIterator it, MessageType message) {
        DataRecord record;
        int count = 0;
        while ((record = it.nextRecord()) != null) {
            count++;
            DataIterators.skipRecord(record, it);
        }
        stat.counter.addAndGet(count);

//        System.out.println(stat.counter.get());
    }

    @Override
    public boolean retrieveMessages(MessageVisitor visitor) {
        super.retrieveMessages(visitor);
        long mask = retrieveMask();
        if (hasMessageMask(mask, MessageType.DESCRIBE_PROTOCOL))
            mask = retrieveDescribeProtocolMessage(visitor, mask);
        MessageType subscrType = VkNetTest.SUBSCRIPTION_TYPE;
        if (hasMessageMask(mask, subscrType) && !visitor.visitSubscription(subscription, subscrType))
            mask = clearMessageMask(mask, subscrType);
        return addMask(mask);
    }

    @Override
    public void setMessageListener(MessageListener listener) {
        super.setMessageListener(listener);
        notifyListener();
    }
}
