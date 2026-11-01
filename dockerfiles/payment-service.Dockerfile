FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /build

COPY dockerfiles/maven-settings.xml /tmp/maven-settings.xml
COPY payment-service/pom.xml ./pom.xml
COPY payment-service/src ./src
RUN --mount=type=cache,id=mini-wallet-maven,target=/root/.m2,sharing=locked \
    mvn --settings /tmp/maven-settings.xml --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN groupadd --gid 10001 wallet \
    && useradd --uid 10001 --gid wallet --no-create-home --shell /usr/sbin/nologin wallet
COPY --from=build --chown=wallet:wallet /build/target/*-SNAPSHOT.jar ./app.jar
USER wallet
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8082
HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=8 \
    CMD curl --fail --silent http://localhost:8082/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
