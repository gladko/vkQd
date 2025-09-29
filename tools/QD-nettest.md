qds tool contains nettest tool.



## case 1 "replication"
1M symbols
1 producer -> 1 MUX -> 2 MUX -> 6 consumers

```
# producer
./qds nettest -s 10 -S 1000000 -l replication-producer.log p localhost:7001

# tier-1 MUX
./qds multiplexor --stat 10s --log mux-tier-1.log  :7001 :7002

# 2 tier-2 MUX
./qds multiplexor --stat 10s --log mux-tier-2-1.log localhost :7002 :7003
./qds multiplexor --stat 10s --log mux-tier-2-2.log localhost :7002 :7004

# consumers
./qds nettest -C 3 -s 10 -S 1000000 -l replication-consumer1.log c localhost:7003
./qds nettest -C 3 -s 10 -S 1000000 -l replication-consumer2.log c localhost:7004
```

## case 2 sharding
1M symbols 
1 producer -> 2 MUX with sharding -> 6 consumers

```
# producer
./qds nettest -s 10 -S 1000000 p "(localhost:7001)(localhost:7003)"

# 2 tier-2 MUX
./qds multiplexor --stat 10s --log mux-shard-1.log :7001 :7002
./qds multiplexor --stat 10s --log mux-shard-2.log :7003 :7004

# consumers
./qds nettest -C 6 -s 10 -l sharding-consumer.log -S 1000000 c "(hash0of2@localhost:7002)(hash1of2@localhost:7004)"
```