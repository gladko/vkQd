package app;

import com.devexperts.qd.QDDistributor;
import com.devexperts.qd.ng.AbstractRecordSink;
import com.devexperts.qd.ng.RecordCursor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SubscriptionCollector {
    private final Map<String, Set<String>> subscription = new ConcurrentHashMap<>();

    public SubscriptionCollector(QDDistributor qdDistributor) {
        this(qdDistributor, x -> {});
    }

    public SubscriptionCollector(QDDistributor qdDistributor, Consumer<Map<String, Set<String>>> updateListener) {
        qdDistributor.getAddedRecordProvider().setRecordListener(provider -> {
            provider.retrieve(new AbstractRecordSink() {
                @Override
                public void append(RecordCursor cursor) {
                    Set<String> symbols = subscription.computeIfAbsent(cursor.getRecord().getName(),
                            k -> ConcurrentHashMap.newKeySet());
                    symbols.add(cursor.getDecodedSymbol());
                }
            });
            updateListener.accept(subscription);
        });

        qdDistributor.getRemovedRecordProvider().setRecordListener(provider -> {
            provider.retrieve(new AbstractRecordSink() {
                @Override
                public void append(RecordCursor cursor) {
                    Set<String> symbols = subscription.computeIfAbsent(cursor.getRecord().getName(),
                            k -> ConcurrentHashMap.newKeySet());
                    symbols.remove(cursor.getDecodedSymbol());
                }
            });
            updateListener.accept(subscription);
        });
    }

    public Collection<String> getSubscription(String record) {
        return subscription.getOrDefault(record, Collections.emptySet());
    }

    public Map<String, Set<String>> getSubscription() {
        return subscription;
    }
}
