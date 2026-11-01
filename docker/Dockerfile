FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home spring
COPY --from=build /workspace/target/address-book.jar /app/address-book.jar
USER 10001
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
    CMD ["sh", "-c", "curl -fsS \"http://localhost:${SERVER_PORT:-8080}/actuator/health/readiness\" || exit 1"]
ENTRYPOINT ["java", "-jar", "/app/address-book.jar"]
