package vk.qds.perf;

import com.devexperts.qd.*;
import com.devexperts.qd.kit.DefaultScheme;
import com.devexperts.qd.ng.*;
import com.devexperts.qd.stats.QDStats;
import com.dxfeed.api.impl.DXFeedScheme;

import java.util.ArrayList;
import java.util.List;


public class QDUtil {
	private QDUtil() {
	}

	public static DefaultScheme getScheme() {
		return DXFeedScheme.getInstance();
	}

	public static List<QDAgent> createAgentsWithSeparateSubscription(int agentsCount, QDCollector qdCollector,
															ClientSubscriber subscriber,
															RecordListener recordListener)
	{
		RecordBuffer totalSubscription = RecordBuffer.getInstance(RecordMode.addedSubscriptionFor(qdCollector.getContract()));
		subscriber.subscribe(totalSubscription);

		List<QDAgent> agents = new ArrayList<>(agentsCount);
		int agentSubscriptionSize = Math.max(totalSubscription.size() / agentsCount, 1);
		for (int i = 0; i < agentsCount; i++) {
			int from = i * agentSubscriptionSize;
			int to = Math.min((i + 1) * agentSubscriptionSize, totalSubscription.size());
			if (from >= to)
				break;
			RecordSource agentSubscription = totalSubscription.newSource(from, to);
			agents.add(QDUtil.createQDAgent(qdCollector,
					subscriptionBuffer -> agentSubscription.retrieve(subscriptionBuffer),
					recordListener));
		}
//		subscriptionBuffer.process(agentSubscription)

		totalSubscription.release();
		return agents;
	}

	public static List<QDAgent> createQDAgents(int count, QDCollector qdCollector,
											   ClientSubscriber subscriber,
											   RecordListener recordListener)
	{
		List<QDAgent> agents = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			agents.add(createQDAgent(qdCollector, subscriber, recordListener));
		}
		return agents;
	}

	public static QDAgent createQDAgent(QDCollector qdCollector,
										ClientSubscriber subscriber,
										RecordListener recordListener)
	{
		QDAgent agent = qdCollector.agentBuilder().build();
		RecordMode subscriptionMode = RecordMode.addedSubscriptionFor(qdCollector.getContract());
		if (recordListener != null) {
			agent.setRecordListener(recordListener);
		}
		addSubscription(agent, subscriptionMode, subscriber);
		return agent;
	}

	public static QDAgent createQDAgent(QDCollector qdCollector, ClientSubscriber subscriber) {
		return createQDAgent(qdCollector, subscriber, RecordListener.VOID);
	}

	public static void addSubscription(QDAgent agent, RecordMode subscriptionMode, ClientSubscriber subscriber) {
		RecordBuffer sb = RecordBuffer.getInstance(subscriptionMode);
		subscriber.subscribe(sb);
		agent.addSubscription(sb);
		sb.release();
	}

	public static void removeSubscription(QDAgent agent, RecordMode subscriptionMode, ClientSubscriber subscriber) {
		RecordBuffer sb = RecordBuffer.getInstance(subscriptionMode);
		subscriber.subscribe(sb);
		agent.removeSubscription(sb);
		sb.release();
	}

	public static void setSubscription(QDAgent agent, RecordMode subscriptionMode, ClientSubscriber subscriber) {
		RecordBuffer sb = RecordBuffer.getInstance(subscriptionMode);
		subscriber.subscribe(sb);
		agent.setSubscription(sb);
		sb.release();
	}

	public static void publish(QDDistributor distributor, OutputRecordsProvider outputRecordsProvider) {
		RecordBuffer outputBuffer = RecordBuffer.getInstance();
		outputRecordsProvider.provideRecords(outputBuffer);
		distributor.process(outputBuffer);
		outputBuffer.release();
	}

	public static QDTicker createTicker(QDStats stats) {
		return QDFactory.getDefaultFactory()
				.tickerBuilder()
				.withScheme(getScheme())
				.withStats(stats)
				.build();
	}


	@FunctionalInterface
	public interface ClientSubscriber {
		void subscribe(RecordBuffer subscriptionBuffer);
	}

	@FunctionalInterface
	public interface OutputRecordsProvider {
		void provideRecords(RecordBuffer outputBuffer);
	}
}
