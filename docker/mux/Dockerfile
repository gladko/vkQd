FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY qds /app
COPY libs /app/libs

ENV CLASSPATH=libs/3.338/qds-tools-3.338.jar:libs/3.338/qds-3.338.jar:libs/3.338/dxlib-3.338.jar:libs/3.338/qds-file-3.338.jar:libs/3.338/mars-3.338.jar:libs/3.338/qds-monitoring-3.338.jar:libs/3.338/dxfeed-api-3.338.jar:libs/config-1.4.3.jar:libs/vk-qds-tools-0.0.1.jar

# Doesn't work
# CMD ./qds multiplexor --stat 10s :7000 :5000

# WORKS
#CMD java -cp $CLASSPATH com.devexperts.qd.tools.Tools multiplexor --stat 10s :7000 :5000

CMD java -cp $CLASSPATH com.devexperts.qd.tools.Tools multiplexor --stat 10s "(:7000)$UP_STREAM_ADDRESS" :5000