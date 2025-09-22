Test compares QDS and JDK map implementation using as a keys various sets of symbols: 
- stock instrument symbols
- stock and option instrument symbols
- symbols that not quite stock and option instrument symbols (includes some symbols that start with numbers)
  // TODO: test for arbitrary symbols

Summary: 
- MatrixSymbolObjectMap has a worse performance comparing to JDK HashMap. We should avoid using it.
- IndexedMap performs better in GET operation. It matters!

```
## stocks

### PUT 10k symbols
QD MatrixSymbolObjectMap:         1.413449130949131 ms
QD MatrixSymbolObjectMap + cusip: 1.2375571470111701 ms
HashMap:                          1.0414082264082265 ms
ConcurrentHashMap:                1.8873658036301715 ms
QD IndexedMap:                    2.2289578972912305 ms

### GET 10k symbols
QD MatrixSymbolObjectMap:         0.6029360028785316 ms
QD MatrixSymbolObjectMap + cusip: 0.23881467674571125 ms
HashMap:                          0.40943507771093973 ms
ConcurrentHashMap:                0.458210932981048 ms
QD IndexedMap:                    0.48607465354591795 ms


## Real stocks and options

### PUT 10k symbols
QD MatrixSymbolObjectMap:         9.364793309368192 ms
QD MatrixSymbolObjectMap + cusip: 10.179789943355118 ms
HashMap:                          2.1735003812636164 ms
ConcurrentHashMap:                3.2012047821350764 ms
QD IndexedMap:                    3.149699474945534 ms

### GET 10k symbols
QD MatrixSymbolObjectMap:         2.825507918663762 ms
QD MatrixSymbolObjectMap + cusip: 2.7899044364560637 ms
HashMap:                          1.2502817785039941 ms
ConcurrentHashMap:                1.2229905787944808 ms
QD IndexedMap:                    1.080950074074074 ms


## Fake stocks and options

### PUT 10k symbols
QD MatrixSymbolObjectMap:         12.235400286129266 ms
QD MatrixSymbolObjectMap + cusip: 12.431189784313725 ms
HashMap:                          3.8947023725490197 ms
ConcurrentHashMap:                4.6865712752360205 ms
QD IndexedMap:                    3.8057224502541755 ms

### GET 10k symbols
QD MatrixSymbolObjectMap:         4.23082992302106 ms
QD MatrixSymbolObjectMap + cusip: 4.097279705882353 ms
HashMap:                          2.2265447610748 ms
ConcurrentHashMap:                2.3370130341321715 ms
QD IndexedMap:                    2.280387098039216 ms


## Arbitrary symbols

### PUT 10k symbols
QD MatrixSymbolObjectMap:         10.000968684444445 ms
QD MatrixSymbolObjectMap + cusip: 9.286662548148149 ms
HashMap:                          2.7001655481481484 ms
ConcurrentHashMap:                2.859409855555555 ms
QD IndexedMap:                    3.1177031155555555 ms

### GET 10k symbols
QD MatrixSymbolObjectMap:         3.1053204074074072 ms
QD MatrixSymbolObjectMap + cusip: 3.161261081481481 ms
HashMap:                          1.3216520822222224 ms
ConcurrentHashMap:                1.4217743096296296 ms
QD IndexedMap:                    1.331999174074074 ms
```