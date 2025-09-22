# Sandbox for studying QD-based apps performance.

## Goal
Practise with QD as well as app performance analysis and improvement  

## Given
3 apps running in separate processes:
- Client: subscribes on `Greeks.Volatility` field of DXFeedScheme
- VolatilityCalculator: calculates and distributes Greeks.Volatility
- QuotesGenerator: produces Quotes that are needed for Volatility calculation

## Desired
Maximum throughput of Greeks.Volatility distribution. Client should report number of 
received Volatility values. This number must be as big as possible.

## Requirements
- Communication over QD
- Client component has several (20) "consumers" with dedicated QDAgent 
- Subscription size: 100k symbols
- Quote distribution frequency: at least 100 times per second (Allowed to push quotes partially but without starvation of individual symbol). 
- Client must process NONE-EMPTY updates at least 50 times per second. (Client UI has to blinking)


## Greeks.Volatility calculation function:
```
    private static final int CALC_ITERATIONS = 300;
    
    private double calcVolatility(double bidPrice) {
        // A simple computation to simulate CPU work
        long result = 0;

        for (int i = 1; i <= CALC_ITERATIONS; i++) {
            result += Math.pow(bidPrice, 2) * Math.sqrt(i);
        }
        return result;
    }
```
where bidPrice is `Quote.Bid.Price` field record in DXFeedScheme.


## Issues to be introduced, showed in monitoring/profiling tools and solved
- High disk usage: logging to file in Calculator and Client
- High io: replace host IP on localhost
- High io: huge symbols 
- High GC: Double volatility, bidPrice, object Quote, iterator
- Quote distribution requirement: are we sure regarding 100k times per second? Connection monitoring is needed.

## Improvements
- add calc threads
- add connections
- collector striping
- connection striping
- custom MessageAdapter in Client and QuoteGenerator
- replay prepared Quotes in QuoteGenerator
- replace ticker to stream