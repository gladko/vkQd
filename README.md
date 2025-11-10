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
3. in QDS/pom.xml change jmxtool version on actual value (that you have found)
4. run `mvn clean package` in QDS directory


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