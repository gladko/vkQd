
## vknettest vs original nettest
```
# producer
./qds vknettest -S 1000000 --stat 10 --log vk-producer.log p :7000
# or
./qds vknettest -S 1000000 -g seq --stat 10 --log vk-seq-producer.log p :7000

# consumers
./qds vknettest -S 1000000 -C 6 --stat 10 --log vk-consumer.log c localhost:7000
```

```
./qds nettest -S 1000000 --stat 10 --log producer.log p :7000
./qds nettest -S 1000000 -C 6 --stat 10 --log consumer1.log c localhost:7000
```

## Kill ALL
`kill -9 $(ps -ef | grep com.devexperts.qd.tools.Tools | awk '{print $2}')`
