package app.v2;

public class VolatilityCalculator {

    // TODO: what has to be changed in original version???
    // TODO: replace active pooling to wait/notify, play with WORKERS_COUNT, VOLATILITY_CALC_COMPLEXITY, ...
    // TODO: use QDS API classes like QdEndpoint, MonitoredQDEndpoint, SubscriptionProcessor
    public static void main(String[] args) throws InterruptedException {
        app.v1.VolatilityCalculator.main(args);
    }
}
