package intro.feed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXPublisher;
import com.dxfeed.event.market.Quote;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DxFeedProducer {
    private final DXPublisher publisher;

    public DxFeedProducer(DXPublisher publisher) {
        this.publisher = publisher;
    }

    public void publishQuotes(String symbol) throws InterruptedException {
        while (true) {
            Quote quote = new Quote(symbol);
            quote.setBidPrice(ThreadLocalRandom.current().nextDouble(100));
            publisher.publishEvents(List.of(quote));
            Thread.sleep(100);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        DXEndpoint publishedEndpoint = DXEndpoint.create(DXEndpoint.Role.PUBLISHER);
        DXPublisher publisher = publishedEndpoint
                .connect(":" + 8000)
                .getPublisher();

        DxFeedProducer producer = new DxFeedProducer(publisher);
        producer.publishQuotes("IBM");

        Thread.sleep(Long.MAX_VALUE);
    }

}
