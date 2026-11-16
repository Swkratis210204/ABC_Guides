# syntax=docker/dockerfile:1
# Build context is the repository root (so the image can include data-seed/):
#   docker build -f backend/Dockerfile .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY backend/src ./src
# Tests run in development/CI (they read ../data-seed), not inside the image build.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache su-exec && addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=build /build/target/deptinfo.jar app.jar
# Starting data. Copied into /data only when the data volume is empty (first boot).
COPY data-seed /app/seed
COPY backend/docker-entrypoint.sh /app/entrypoint.sh
RUN sed -i 's/\r$//' /app/entrypoint.sh && chmod +x /app/entrypoint.sh

ENV DATA_DIR=/data \
    SEED_DIR=/app/seed \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"UP"' || exit 1
ENTRYPOINT ["/app/entrypoint.sh"]
