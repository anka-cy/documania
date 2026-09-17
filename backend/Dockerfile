# syntax=docker/dockerfile:1.7
#
# documania v2 — image unique (API + SPA statique servie par Spring Boot).
# Multi-stage : compilation Maven (Java 21) puis exécution JRE minimal.

FROM docker.io/library/maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM docker.io/library/eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app app

WORKDIR /app

COPY --from=build --chown=app:app /workspace/target/backend-0.0.1-SNAPSHOT.jar ./app.jar

USER 10001:10001

EXPOSE 8080

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/ > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
