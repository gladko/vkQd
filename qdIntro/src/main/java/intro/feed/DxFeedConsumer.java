package intro.feed;

import com.dxfeed.api.DXEndpoint;
import com.dxfeed.api.DXFeed;
import com.dxfeed.api.DXFeedSubscription;
import com.dxfeed.event.market.Quote;

import java.util.List;

public class DxFeedConsumer {
    private final DXFeed dxFeed;
    private final DXFeedSubscription<Quote> quoteSubscription;

    public DxFeedConsumer(DXFeed dxFeed) {
        this.dxFeed = dxFeed;
        quoteSubscription = dxFeed.createSubscription(Quote.class);
        quoteSubscription.addEventListener(this::onQuoteAvailable);
    }

    private void onQuoteAvailable(List<Quote> quotes) {
        for (Quote quote : quotes) {
            System.out.println(quote);
            System.out.println(quote.getBidPrice());
        }
    }

    public void subscribeQuotes(String symbol) {
        quoteSubscription.addSymbols(symbol);
    }

    public static void main(String[] args) throws InterruptedException {
//        DXFeed dxFeed = DXFeed.getInstance();

        DXEndpoint endpoint = DXEndpoint.create(DXEndpoint.Role.FEED);
        DXFeed feed = endpoint
                .connect("localhost:8000")
                .getFeed();

        DxFeedConsumer consumer = new DxFeedConsumer(feed);
        consumer.subscribeQuotes("IBM");

        Thread.sleep(Long.MAX_VALUE);
    }

}
