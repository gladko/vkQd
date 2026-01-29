## QD address as yaml file

with python ...
```bash
./qds multiplexor --stat 10s "$(./configs/parse.py muxconfig.yaml distributorAddress)" "$(./configs/parse.py muxconfig.yaml agentAddress)"

# or with qdConfigServer.py running...
./qds multiplexor --stat 10s "$(curl localhost:9999/distributor)" "$(curl localhost:9999/agent)"
```
or with Parser.java
```bash
# mkdir configs/build && javac -cp "configs/snakeyaml-2.1.jar" -sourcepath "configs" -d "configs/build" configs/Parser.java
# PARSER="java -cp 'configs/snakeyaml-2.1.jar;configs/build' Parser "
PARSER=$JAVA_25/bin/java -cp "configs/snakeyaml-2.1.jar;." configs/Parser.java configs/muxconfig.yaml agentAddress
./qds multiplexor --stat 10s "$(eval $PARSER  configs/muxconfig.yaml distributorAddress)" "$(eval $PARSER configs/muxconfig.yaml agentAddress)"
```

Usually client connection is configured by property with OPPOSITE side name.
client `consuming` connection is configured via PRODUCER_ADDRESS property.

While service (server) side apps are configured by properties with service-own-name
`fooService` -> FOO_SERVICE_PORT

Now let's look at QD props:

DISTRIBUTOR_ADDRESS setups
  - serverConsumer aka RECEIVER. Passively accepts incoming data from upstream
  - clientConsumer. Actively takes data from upstream

AGENT_ADDRESS setups
  - serverProvider. Provides data to downstream
  - clientProvider. Pushes data to upstream
