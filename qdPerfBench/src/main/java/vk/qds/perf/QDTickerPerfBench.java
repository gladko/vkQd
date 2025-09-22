package vk.qds.perf;

import com.devexperts.qd.QDAgent;
import com.devexperts.qd.QDDistributor;
import com.devexperts.qd.QDTicker;
import com.devexperts.qd.kit.DefaultRecord;
import com.devexperts.qd.kit.DefaultScheme;
import com.devexperts.qd.ng.RecordBuffer;
import com.devexperts.qd.ng.RecordCursor;
import com.devexperts.qd.ng.RecordListener;
import com.devexperts.qd.ng.RecordProvider;
import com.devexperts.qd.stats.QDStats;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class QDTickerPerfBench {
	private static boolean SHOW_ITERATION_RESULTS = false;
	private static boolean SHOW_TOTAL_RESULTS = false;
	private static boolean SHOW_RECEIVED_RESULTS = false;

	private static final DefaultScheme scheme = QDUtil.getScheme();

	private static final DefaultRecord QUOTE = scheme.findRecordByName("Quote");
	private static final DefaultRecord TRADE = scheme.findRecordByName("Trade");
	private static final DefaultRecord THEO_PRICE = scheme.findRecordByName("TheoPrice");

	private static final int QUOTE_BID_INDEX = QUOTE.findFieldByName("Quote.Bid.Price").getIndex();


	private ScheduledExecutorService reporter;
	private ScheduledExecutorService agentExecutorService;
	private List<String> symbols;
	private QDTicker ticker;
	private List<QDAgent> qdAgents;
	private AtomicLong publishCounter = new AtomicLong();
	private AtomicLong receivedCounter = new AtomicLong();

	private ResultsCollector resultsCollector;
	private TestParameters parameters;

	public static void main(String[] args) throws InterruptedException {
		QDTickerPerfBench perfBench = new QDTickerPerfBench();
		perfBench.init(new TestParameters()
				.setAgentsCount(10)
				.setAgentSeparateSubscription(true));

		System.out.println(perfBench.qdAgents);

		perfBench.testRunImpl(timeLimiter(10));
		perfBench.afterEach();
	}

	void init(TestParameters parameters) {
		this.parameters = parameters;
		this.symbols = SymbolUtil.createRealSymbols(parameters.instrListCount);
		ticker = QDUtil.createTicker(QDStats.VOID);
		publishCounter = new AtomicLong();
		receivedCounter = new AtomicLong();
		reporter = Executors.newSingleThreadScheduledExecutor();
		resultsCollector = new ResultsCollector();

		qdAgents = createQDAgents();
	}

	@BeforeAll
	static void warmup() throws InterruptedException {
		System.out.println("\nwarm up");
		QDTickerPerfBench bench = new QDTickerPerfBench();
		bench.symbolsCountTest(new TestParameters());
		bench.afterEach();
	}

	@AfterEach
	void afterEach() {
		showResults();

		ticker.close();
		reporter.shutdown();
		if (agentExecutorService != null)
			agentExecutorService.shutdown();
		parameters = null;
	}


	private static Stream<TestParameters> symbolsCountTestParams() {
		return Stream.of(
				new TestParameters().setInstrListCount(100).setAgentsCount(1),
				new TestParameters().setInstrListCount(1_000).setAgentsCount(1),
				new TestParameters().setInstrListCount(10_000).setAgentsCount(1),
				new TestParameters().setInstrListCount(100).setAgentsCount(5),
				new TestParameters().setInstrListCount(1_000).setAgentsCount(5),
				new TestParameters().setInstrListCount(10_000).setAgentsCount(5));
	}

	@ParameterizedTest
	@MethodSource("symbolsCountTestParams")
	public void symbolsCountTest(TestParameters params) throws InterruptedException {
		System.out.println("\n- instrListCount=" + params.instrListCount + ", agentsCount=" + params.agentsCount);
		init(params);
		testRunImpl(timeLimiter(10));
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 10, 100, 1000, 10_000, 50_000})
	public void packageSizeTest(int packageSize) throws InterruptedException {
		System.out.println("\n- packageSize=" + packageSize);
		init(new TestParameters().setPackageSize(packageSize));
		testRunImpl(timeLimiter(20));
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 3, 10, 100, 300, 500, 1000})
	public void agentCountTest_50kSymbols(int agentCount) throws InterruptedException {
		System.out.println("\n- agentsCount=" + agentCount);
		init(new TestParameters()
				.setInstrListCount(1000)
				.setAgentsCount(agentCount));
		testRunImpl(timeLimiter(10));
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 3, 10, 100, 300, 500, 1000, 10_000})
	public void agentCountTest_5kSymbols(int agentCount) throws InterruptedException {
		System.out.println("\n- agentsCount=" + agentCount);
		init(new TestParameters()
				.setInstrListCount(100)
				.setAgentsCount(agentCount));
		testRunImpl(timeLimiter(10));
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 3, 5, 10})
	public void distributorsCountTest_commonSubscription(int distributorsCount) throws InterruptedException {
		System.out.println("\n- distributorsCount=" + distributorsCount);
		init(new TestParameters()
				.setAgentsCount(100)
				.setAgentSeparateSubscription(false)
				.setDistributorsCount(distributorsCount));
		testRunImpl(timeLimiter(10));
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 3, 5, 10})
	public void distributorsCountTest_separateSubscription(int distributorsCount) throws InterruptedException {
		System.out.println("\n- distributorsCount=" + distributorsCount);
		init(new TestParameters()
				.setAgentsCount(100)
				.setAgentSeparateSubscription(true)
				.setDistributorsCount(distributorsCount));
		testRunImpl(timeLimiter(10));
	}

	@ParameterizedTest
	@MethodSource("separateThreadReceivingTestParams")
	public void separateThreadReceivingTest(TestParameters params) throws InterruptedException {
		System.out.println("\n- agentSeparateThreadReceiving=" + params.agentSeparateThreadReceiving
				+ ", agentsCount=" + params.agentsCount
				+ ", distributors=" + params.distributorsCount);

		init(params);
		testRunImpl(timeLimiter(10));
	}

	private static Stream<TestParameters> separateThreadReceivingTestParams() {
		return Stream.of(
				new TestParameters()
					.setAgentSeparateThreadReceiving(false)
					.setAgentsCount(1)
					.setDistributorsCount(1),

				new TestParameters()
					.setAgentSeparateThreadReceiving(true)
					.setAgentsCount(1)
					.setDistributorsCount(1),

				new TestParameters()
					.setAgentSeparateThreadReceiving(false)
					.setAgentsCount(100)
					.setDistributorsCount(1),

				new TestParameters()
					.setAgentSeparateThreadReceiving(true)
					.setAgentsCount(100)
					.setDistributorsCount(1),

				new TestParameters()
					.setAgentsCount(100)
					.setAgentSeparateThreadReceiving(false)
					.setDistributorsCount(5),

				new TestParameters()
					.setAgentSeparateThreadReceiving(true)
					.setAgentsCount(100)
					.setDistributorsCount(5));
	}


	static Supplier<Boolean> iterationsLimiter(int i) {
		AtomicInteger counter = new AtomicInteger(i);
		return ()-> counter.decrementAndGet() >= 0;
	}

	static Supplier<Boolean> timeLimiter(long duration) {
		long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(duration);
		return ()-> System.nanoTime() < end;
	}

	void testRunImpl(Supplier<Boolean> active) throws InterruptedException {
		ScheduledFuture<?> reportTask =
				reporter.scheduleAtFixedRate(this::collectResults, 1, 1, TimeUnit.SECONDS);

		CountDownLatch latch = new CountDownLatch(parameters.distributorsCount);
		for (int i = 0; i < parameters.distributorsCount; i++) {
			new Thread(() -> {
				QDDistributor qdDistributor = ticker.distributorBuilder().build();

				while (active.get()) {
					QDUtil.publish(qdDistributor, buffer -> addQuotes(buffer, parameters.packageSize));
					publishCounter.addAndGet(parameters.packageSize);
				}
				reportTask.cancel(false);
				latch.countDown();
			}).start();
		}
		latch.await();
	}

	private List<QDAgent> createQDAgents() {
		RecordListener recordListener = parameters.agentSeparateThreadReceiving
				? provider -> receivedCounter.addAndGet(1)
				: incomingCountingListener();

		List<QDAgent> qdAgents;
		if (parameters.agentSeparateSubscription) {
			qdAgents = QDUtil.createAgentsWithSeparateSubscription(parameters.agentsCount, ticker,
					buffer -> subscribe(buffer, QUOTE, symbols),
					recordListener);
		} else {
			qdAgents = QDUtil.createQDAgents(parameters.agentsCount, ticker,
					buffer -> subscribe(buffer, QUOTE, symbols),
					recordListener);
		}


		if (parameters.agentSeparateThreadReceiving) {
			agentExecutorService = Executors.newSingleThreadScheduledExecutor();
			agentExecutorService.scheduleWithFixedDelay(()-> {
				for (QDAgent qdAgent : qdAgents) {
					RecordListener.VOID.recordsAvailable(qdAgent);
				}
			}, 1, 100, TimeUnit.MILLISECONDS);
		}
		return qdAgents;
	}

	private void collectResults() {
		ResultItem res = takeResults();
		if (SHOW_ITERATION_RESULTS) {
			show("iteration " + resultsCollector.size(), res, true);
		}
		if (res.received == 0) {
			String msg = "Received is ZERO! " + res;
			System.out.println(msg);
			throw new RuntimeException(msg);
		}

		resultsCollector.add(res);
	}

	private ResultItem takeResults() {
		return new ResultItem(publishCounter.getAndSet(0), receivedCounter.getAndSet(0));
	}

	private void showResults() {
		if (SHOW_TOTAL_RESULTS) {
			show("TOTAL", resultsCollector.totalResult(), SHOW_RECEIVED_RESULTS);
		}
		show("AVG  ", resultsCollector.avgResult(), SHOW_RECEIVED_RESULTS);
	}

	private void show(String msg, ResultItem results, boolean showReceived) {
		msg += "   " + results.format(showReceived);
		System.out.println(msg);
	}

	public ResultsCollector getResults() {
		return resultsCollector;
	}


	// may contain duplicate symbols. It does not matter in distributing prospective.
	// Because duplicates have to be inserted into collector as well.
	private void addQuotes(RecordBuffer buffer, int count) {
		Random random = ThreadLocalRandom.current();
		for (int i = 0; i < count; i++) {
			String symbol = symbols.get(random.nextInt(symbols.size()));
			RecordCursor cur = buffer.add(QUOTE, scheme.getCodec().encode(symbol), symbol);
			cur.setInt(QUOTE_BID_INDEX, random.nextInt());
		}
	}

	private RecordListener incomingCountingListener() {
		return recordProvider -> processIncoming(recordProvider, receivedCounter);
	}

	private static void processIncoming(RecordProvider recordProvider, AtomicLong counter) {
		RecordBuffer buffer = RecordBuffer.getInstance();
		recordProvider.retrieve(buffer);
		counter.addAndGet(buffer.size());
		buffer.release();
	}

	public static void subscribe(RecordBuffer buf, DefaultRecord rec, Collection<String> symbols) {
		for (String symbol : symbols) {
			subscribe(buf, rec, symbol);
		}
	}

	public static void subscribe(RecordBuffer buf, DefaultRecord rec, String symbol) {
		int cipher = scheme.getCodec().encode(symbol);
		buf.add(rec, cipher, symbol);
	}


	public static class ResultsCollector implements Serializable {
		private final List<ResultItem> results = new CopyOnWriteArrayList<>();

		public ResultItem totalResult() {
			long totalPublished = 0;
			long totalReceived = 0;

			for (ResultItem item : results) {
				totalPublished += item.published;
				totalReceived += item.received;
			}
			return new ResultItem(totalPublished, totalReceived);
		}

		public int size() {
			return results.size();
		}

		public void add(ResultItem item) {
			results.add(item);
		}

		public ResultItem avgResult() {
			return totalResult().avg(results.size());
		}
	}

	public static class ResultItem implements Serializable {
		final long published;
		final long received;

		ResultItem(long published, long received) {
			this.published = published;
			this.received = received;
		}

		public ResultItem avg(int count) {
			return new ResultItem(published / count, received / count);
		}

		public String format(boolean showReceived) {
			String result = "PUBLISHED: " + published;
			if (showReceived) {
				result += "   RECEIVED: " + received;
			}
			return result;
		}

		@Override
		public String toString() {
			return "Results{" +
					"published=" + published +
					", received=" + received +
					'}';
		}
	}

	// TODO: make fields final and use lombok for builder generation
	static class TestParameters implements Serializable {
		int instrListCount = 1000;
		int packageSize = 10;
		int distributorsCount = 1;
		int agentsCount = 1;
		boolean agentSeparateThreadReceiving;
		boolean agentSeparateSubscription;

		TestParameters() {}

		public TestParameters copy() {
			return new TestParameters()
					.setInstrListCount(instrListCount)
					.setPackageSize(packageSize)
					.setDistributorsCount(distributorsCount)
					.setAgentsCount(agentsCount)
					.setAgentSeparateThreadReceiving(agentSeparateThreadReceiving)
					.setAgentSeparateSubscription(agentSeparateSubscription);
		}

		public TestParameters setInstrListCount(int instrListCount) {
			this.instrListCount = instrListCount;
			return this;
		}

		public TestParameters setPackageSize(int packageSize) {
			this.packageSize = packageSize;
			return this;
		}

		public TestParameters setDistributorsCount(int distributorsCount) {
			this.distributorsCount = distributorsCount;
			return this;
		}

		public TestParameters setAgentsCount(int agentsCount) {
			this.agentsCount = agentsCount;
			return this;
		}

		public TestParameters setAgentSeparateThreadReceiving(boolean agentSeparateThreadReceiving) {
			this.agentSeparateThreadReceiving = agentSeparateThreadReceiving;
			return this;
		}

		public TestParameters setAgentSeparateSubscription(boolean agentSeparateSubscription) {
			this.agentSeparateSubscription = agentSeparateSubscription;
			return this;
		}

		@Override
		public String toString() {
			return "TestParameters{" +
					"instrListCount=" + instrListCount +
					", packageSize=" + packageSize +
					", distributorsCount=" + distributorsCount +
					", agentsCount=" + agentsCount +
					", agentSeparateThreadReceiving=" + agentSeparateThreadReceiving +
					", agentSeparateSubscription=" + agentSeparateSubscription +
					'}';
		}
	}
}
