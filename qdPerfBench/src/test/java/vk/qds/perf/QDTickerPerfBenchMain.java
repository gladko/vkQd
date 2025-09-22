package vk.qds.perf;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.svg.SVGGraphics2D;
import org.jfree.svg.SVGUtils;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static vk.qds.perf.QDTickerPerfBench.timeLimiter;
import static vk.qds.perf.QDTickerPerfBenchMain.DistributionPair.distributionPairs;

public class QDTickerPerfBenchMain {
	private static final long EACH_RUN_DURATION = 10;
	public static final String RESULT_STORAGE_DIR = "build/results";
	public static final String RESULTS_REPOSITORY_KEY = "RESULTS_REPOSITORY";
	// can be used for comparing results and for building a chart without running test.
	static List<ResultPair> RESULTS_REPOSITORY = new ArrayList<>();

	// Set of cases suitable for simulation of incoming data insertion
	static SeriesParams REPEATER_INCOMING_DATA = new SeriesParams(
			new QDTickerPerfBench.TestParameters()
					.setInstrListCount(30_000)					// 1.5M instruments
					.setAgentSeparateThreadReceiving(true)
					.setAgentSeparateSubscription(true),		// each worker has a dedicated subscription
			new int[] {1, 10, 20, 30, 100}, 					// worker QDAgents
			distributionPairs("1:100", "1:1000", "1:10000", "1:100000", "1:1000000",
					"3:100", "3:1000", "3:10000", "3:100000",
					"5:100", "5:1000", "5:10000", "5:100000")  		// QDDistributors of incoming data connections : batching
	);

	// Set of cases suitable for simulation outgoing data insertion
	static SeriesParams REPEATER_OUTGOING_DATA = new SeriesParams(
			new QDTickerPerfBench.TestParameters()
					.setInstrListCount(30_000)
					.setAgentSeparateThreadReceiving(true)
					.setAgentSeparateSubscription(true),			// each connection has a dedicated subscription
			new int[] {1, 3, 5, 10, 15},							// QDAgents of outgoing data connections + component internal consumers
			distributionPairs( "1:100", "1:10000", "1:100000", "1:1000000",
					"3:100", "3:10000", "3:100000",		// worker QDDistributors : batching
					"5:100", "5:10000", "5:100000"
//					"10:100", "10:1000", "10:10000", "10:100000",
//					"30:100", "30:1000", "30:10000", "30:100000"
			)
	);

	static SeriesParams TEST = new SeriesParams(
			new QDTickerPerfBench.TestParameters()
					.setInstrListCount(30_000)
					.setAgentSeparateThreadReceiving(true)
					.setAgentSeparateSubscription(true),			// each connection has a dedicated subscription
			new int[] {1, 3, 5, 10, 15},							// QDAgents of outgoing data connections + component internal consumers
			distributionPairs( "1:100", "1:10000", "1:100000", "1:1000000",
					"3:100000",		// worker QDDistributors : batching
					"5:100000"
//					"10:100", "10:1000", "10:10000", "10:100000",
//					"30:100", "30:1000", "30:10000", "30:100000"
			)
	);

	private static QDTickerPerfBench qdPerfBench = new QDTickerPerfBench();


	public static void main(String[] args) throws Exception {
		new File(RESULT_STORAGE_DIR).mkdirs();

		QDTickerPerfBench.warmup();

		try {
//			runTest("TEST", TEST);
			runTest("Repeater_incoming_throughput", REPEATER_INCOMING_DATA);
			runTest("Repeater_outgoing_throughput", REPEATER_OUTGOING_DATA);
		} finally {
			storeResultsToRepository();
		}
	}

	private static void storeResultsToRepository() throws IOException {
		List<ResultPair> totalRepo = SymbolUtil.loadObject(RESULT_STORAGE_DIR, RESULTS_REPOSITORY_KEY);
		totalRepo = totalRepo == null ? new ArrayList<>() : totalRepo;
		totalRepo.addAll(RESULTS_REPOSITORY);
		SymbolUtil.storeObject(RESULT_STORAGE_DIR, RESULTS_REPOSITORY_KEY, totalRepo);
	}


	private static void runTest(String testName, SeriesParams series) throws Exception {
		XYSeriesCollection resultDataSet = new XYSeriesCollection();

		QDTickerPerfBench.TestParameters parameters = series.getTemplate();

		for (DistributionPair distributionPair : series.distributionPairs) {
			parameters.setDistributorsCount(distributionPair.distributorsCount);
			parameters.setPackageSize(distributionPair.packageSize);

			XYSeries currentSeries = new XYSeries(distributionPair.getSeriesKey());
			resultDataSet.addSeries(currentSeries);

			for (int agentCount : series.agentsCounts) {
				parameters.setAgentsCount(agentCount);
				System.out.println("\n- " + parameters);
				try {
					QDTickerPerfBench.ResultsCollector results = runTest(parameters);
					currentSeries.add(parameters.agentsCount, results.avgResult().published);
					RESULTS_REPOSITORY.add(new ResultPair(parameters.copy(), results));
				} catch (Exception e) {
					System.out.println(e);
				}
			}
			SymbolUtil.storeObject(RESULT_STORAGE_DIR, testName + ":" + currentSeries.getKey(), currentSeries);
		}

		writeResultsToSVGImage(testName, resultDataSet);
	}

	private static QDTickerPerfBench.ResultsCollector runTest(QDTickerPerfBench.TestParameters parameters)
			throws InterruptedException
	{
		qdPerfBench.init(parameters);
		qdPerfBench.testRunImpl(timeLimiter(EACH_RUN_DURATION));
		QDTickerPerfBench.ResultsCollector result = qdPerfBench.getResults();
		qdPerfBench.afterEach();
		return result;
	}

	public static void writeResultsToSVGImage(String fileName, XYSeriesCollection dataSet) {
		try {
			JFreeChart chart = ChartFactory.createXYLineChart(
					"QDTicker record insertion throughput",
					"Agents Count","Records Count",
					dataSet,
					PlotOrientation.VERTICAL,true,true,false);

			SVGGraphics2D g2 = new SVGGraphics2D(800, 600);
			Rectangle r = new Rectangle(0, 0, 800, 600);
			chart.draw(g2, r);

			File f = new File(RESULT_STORAGE_DIR, fileName + ".svg");
			SVGUtils.writeToSVG(f, g2.getSVGElement());
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	static class SeriesParams {
		final QDTickerPerfBench.TestParameters template;
		final int[] agentsCounts;
		final DistributionPair[] distributionPairs;

		SeriesParams(QDTickerPerfBench.TestParameters template, int[] agentsCounts,
					 DistributionPair[] distributionPairs)
		{
			this.template = template;
			this.agentsCounts = agentsCounts;
			this.distributionPairs = distributionPairs;
		}

		QDTickerPerfBench.TestParameters getTemplate() {
			return template.copy();
		}
	}

	static class DistributionPair {
		final int distributorsCount;
		final int packageSize;

		DistributionPair(int distributorsCount, int packageSize) {
			this.distributorsCount = distributorsCount;
			this.packageSize = packageSize;
		}

		public String getSeriesKey() {
			return distributorsCount + ":" + packageSize;
		}

		public static DistributionPair[] distributionPairs(String... pairsText) {
			DistributionPair[] pairs = new DistributionPair[pairsText.length];
			for (int i = 0; i < pairsText.length; i++) {
				pairs[i] = DistributionPair.parse(pairsText[i]);
			}
			return pairs;
		}

		public static DistributionPair parse(String text) {
			String[] split = text.split(":");
			return new DistributionPair(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
		}
	}

	public static class ResultPair implements Serializable {
		final QDTickerPerfBench.TestParameters runParams;
		final QDTickerPerfBench.ResultsCollector runResults;

		ResultPair(QDTickerPerfBench.TestParameters runParams, QDTickerPerfBench.ResultsCollector runResults) {
			this.runParams = runParams;
			this.runResults = runResults;
		}
	}
}
