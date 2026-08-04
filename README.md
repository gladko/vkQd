# My personal QDS sandbox

##  building QDS from source
1. download QDS from [github](https://github.com/devexperts/QD)
2. Check if your maven repo contains jmxtools.jar. If not, find it somewhere 
 for instance, [here](https://www.datanucleus.org/downloads/maven2/com/sun/jdmk/jmxtools/1.2.1/).
 Then place it in lib directory and install jmxtools in your local maven repo
    ```bash
    mvn install:install-file \
    -Dfile=libs/jmxtools-1.2.1.jar \
    -DgroupId=com.sun.jdmk \
    -DartifactId=jmxtools \
    -Dversion=1.2.1 \
    -Dpackaging=jar \
    -DgeneratePom=true
    ```
3. in QDS/pom.xml change `jmxtool.version` on actual value (that you have found)
4. run `mvn clean package install` in QDS directory

## Kill all qd tools
```bash
kill -9 $(ps -ef | grep com.devexperts.qd.tools.Tools | awk '{print $2}')
kill -9 $(ps -ef | grep vk.vkPets | awk '{print $2}')
```

## QD project main modules
 - dxlib:   io, logging, JMX, services, util
 - qd-core
 - qd-rmi
 - proto:   base connection abstractions and logic
 - dxFeed-XXX
 - qd-sample
 - benchmarks

## docker playground
### build qds image
```bash
# 1. prepare libs. Make sure Dockerfile specifies desired QDS version 
./gradlew deployQd

# 2. optional: open this directory in WSL. In my case, it's.
cd /mnt/c/Users/vkozak/workspace/projects/vkPets/vkQd

# 3. build qds image
docker build -t qds .
```

### run
```bash
docker network create vkqd-test-network

docker run -d --name mux-root --network vkqd-test-network -p 37010:7010 -p 37015:7015 qds multiplexor --stat 10 :7010 :7015
docker run -d --name mux1 --network vkqd-test-network -p 37115:7015 qds multiplexor --stat 10 mux-root:7015 :7015
docker run -d --name mux2 --network vkqd-test-network -p 37215:7015 qds multiplexor --stat 10 mux-root:7015 :7015

# verify: subscribe on connect to mux1 and post into mux-root. Expected data flows post -> mux-root -> mux1 -> connect
./qds connect localhost:37115 Quote IBM
./qds post localhost:7010
 -> Quote IBM 20:00:11 2 5 6 22:00:11 7

docker run -d --name producer --network vkqd-test-network qds nettest -S 1000000 --stat 10 p mux-root:7010
docker run -d --name consumer1 --network vkqd-test-network qds nettest -C 3 -S 1000000 --stat 10 c mux1:7015
docker run -d --name consumer2 --network vkqd-test-network qds nettest -C 3 -S 1000000 --stat 10 c mux2:7015
```