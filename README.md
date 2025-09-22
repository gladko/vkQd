# My personal QDS sandbox

##  building QDS from source
1. download QDS from https://github.com/devexperts/QD
2. find jmxtools.jar. In `libs` directory there is jmxtool.1.2.1.jar. It should be fine.
3. change jmxtool version in QDS/pom.xml
4. install jmxtools to local maven repo
```
mvn install:install-file \
-Dfile=~\Downloads\jmxtools-1.2.1.jar \
-DgroupId=com.sun.jdmk \
-DartifactId=jmxtools \
-Dversion=1.2.1 \
-Dpackaging=jar \
-DgeneratePom=true
```
5. run `mvn clean package` in QDS directory


# DX QD records
Quote
Trade
TradeETH
Summary
Fundamental
Profile
Order
AnalyticOrder
SpreadOrder
MarketMaker
TimeAndSale
OptionSale
TradeHistory
Candle
Message
Configuration
Greeks
TheoPrice
Underlying
Series