# QDS snapshots live in the local maven repo only, so the jars cannot be resolved
# from inside the image. Collect them into the build context on the host first:
#   ./gradlew deployQd
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY libs/3.347-SNAPSHOT libs/
COPY libs/jmxtools-1.2.8.jar libs/

ENTRYPOINT ["java", "-cp", "/app/libs/*", "com.devexperts.qd.tools.Tools"]
