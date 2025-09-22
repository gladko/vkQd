package app;

import com.devexperts.qd.QDDistributor;
import com.devexperts.qd.ng.AbstractRecordSink;
import com.devexperts.qd.ng.RecordCursor;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SubscriptionHolder {
    private final Set<String> subscription = ConcurrentHashMap.newKeySet();

    SubscriptionHolder(QDDistributor qdDistributor, Consumer<Set<String>> updateListener) {
        qdDistributor.getAddedRecordProvider().setRecordListener(provider -> {
            provider.retrieve(new AbstractRecordSink() {
                @Override
                public void append(RecordCursor cursor) {
                    subscription.add(cursor.getDecodedSymbol());
                }
            });
            updateListener.accept(subscription);
        });

        qdDistributor.getRemovedRecordProvider().setRecordListener(provider -> {
            provider.retrieve(new AbstractRecordSink() {
                @Override
                public void append(RecordCursor cursor) {
                    subscription.remove(cursor.getDecodedSymbol());
                }
            });
            updateListener.accept(subscription);
        });
    }

    public int size() {
        return subscription.size();
    }

    public Collection<String> getSubscription() {
        return subscription;
    }
}
