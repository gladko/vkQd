Result is average number of QD records items pushed to ticker per second. Each test case lasts 10 seconds.

// TODO: 
- add CPU time measurement
- QD record throughput / per CPU clock - efficiency usage of CPU
- CPU profiling
- additional perf metrics like context switches, IPC, CPU cache / memory usage
- consider better result visualization (chart) 


## symbolsCountTest:
COMMON: packageSize=10, distributorsCount=1, agentSeparateThreadReceiving=false, agentSeparateSubscription=false

- instrListCount=100, agentsCount=1
  AVG  	PUBLISHED: 2745767

- instrListCount=1000, agentsCount=1
  AVG  	PUBLISHED: 1550717

- instrListCount=10000, agentsCount=1
  AVG  	PUBLISHED: 1069520

- instrListCount=100, agentsCount=5
  AVG  	PUBLISHED: 1331332

- instrListCount=10000, agentsCount=5
  AVG  	PUBLISHED: 611925

Summary: number of symbols (records) in a matrix of QD collector and QDAgents matters.
Probably, the more symbols the more difficult to calculate index for inserting a record into matrix.

## packageSizeTest
COMMON: instrListCount=1000, agentsCount=1, distributorsCount=1, agentSeparateThreadReceiving=false, agentSeparateSubscription=false

- packageSize=1
  AVG  	PUBLISHED: 746459

- packageSize=10
  AVG  	PUBLISHED: 1687019

- packageSize=100
  AVG  	PUBLISHED: 1836210

- packageSize=1000
  AVG  	PUBLISHED: 1686000

- packageSize=10000
  AVG  	PUBLISHED: 1558500

- packageSize=50000
  AVG  	PUBLISHED: 1452500

Summary: interesting! Initially increasing package size improves performance, but then decreases it! Wondering why.
Probably, the problem with CPU caches or memory. A huge package does not fit to CPU cache size.
Or such a huge package exceeds capacity of some QD internal buffer.
Requires additional investigation.


## agentCountTest. 50k symbols
COMMON: packageSize=10, instrListCount=1000, distributorsCount=1, agentSeparateThreadReceiving=false, agentSeparateSubscription=false

- agentsCount=1
  AVG  	PUBLISHED: 1653517

- agentsCount=10
  AVG  	PUBLISHED: 517802

- agentsCount=100
  AVG  	PUBLISHED: 62276

- agentsCount=300
  AVG  	PUBLISHED: 15344

- agentsCount=500
  AVG  	PUBLISHED: 7870

- agentsCount=1000
  AVG  	PUBLISHED: 1884
  Sometimes happened: Exception in thread "Thread-4" java.lang.OutOfMemoryError: Java heap space
  1000 agents with 50k symbols in subscription is too much


## agentCountTest. 5k symbols
COMMON: packageSize=10, instrListCount=100, distributorsCount=1, agentSeparateThreadReceiving=false, agentSeparateSubscription=false

- agentsCount=1
  AVG  	PUBLISHED: 2747943

- agentsCount=3
  AVG  	PUBLISHED: 1866247

- agentsCount=10
  AVG  	PUBLISHED: 928794

- agentsCount=100
  AVG  	PUBLISHED: 74084

- agentsCount=300
  AVG  	PUBLISHED: 18975

- agentsCount=500
  AVG  	PUBLISHED: 10818

- agentsCount=1000
  AVG  	PUBLISHED: 4862

- agentsCount=10000
  AVG  	PUBLISHED: 245


## distributorsCountTest_commonSubscription
COMMON: packageSize=10, instrListCount=1000, agentsCount=100, agentSeparateThreadReceiving=false, agentSeparateSubscription=false

- distributorsCount=1
  AVG  	PUBLISHED: 57222

- distributorsCount=3
  AVG  	PUBLISHED: 111867

- distributorsCount=5
  AVG  	PUBLISHED: 119778

- distributorsCount=10
  AVG  	PUBLISHED: 118674


## distributorsCountTest_separateSubscription
COMMON: packageSize=10, instrListCount=1000, agentsCount=100, agentSeparateThreadReceiving=false, agentSeparateSubscription=true

- distributorsCount=1
  AVG     PUBLISHED: 2016542

- distributorsCount=3
  AVG     PUBLISHED: 2204551

- distributorsCount=5
  AVG     PUBLISHED: 2592273

- distributorsCount=10
  AVG     PUBLISHED: 2482487


## separateThreadReceivingTest
COMMON: packageSize=10, instrListCount=1000, agentSeparateSubscription=false

- agentSeparateThreadReceiving=false, agentsCount=1, distributors=1
  AVG  	PUBLISHED: 1478877

- agentSeparateThreadReceiving=true, agentsCount=1, distributors=1
  AVG  	PUBLISHED: 1499856

- agentSeparateThreadReceiving=false, agentsCount=100, distributors=1
  AVG  	PUBLISHED: 58127

- agentSeparateThreadReceiving=true, agentsCount=100, distributors=1
  AVG  	PUBLISHED: 62753

- agentSeparateThreadReceiving=false, agentsCount=100, distributors=5
  AVG  	PUBLISHED: 122895

- agentSeparateThreadReceiving=true, agentsCount=100, distributors=5
  AVG  	PUBLISHED: 73352

Summary: processing incoming data separately gives some performance speedup with 1 distributor.
Wondering why it's not true with distributors=5 !!!   122895 vs 73352

