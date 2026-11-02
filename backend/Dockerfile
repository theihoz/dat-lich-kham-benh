# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 clinic
WORKDIR /app
COPY --from=build /src/target/backend-0.0.1-SNAPSHOT.jar app.jar
USER clinic
# Mặc định chạy demo (H2 trong RAM, không cần MySQL/Redis/RabbitMQ).
ENV SPRING_PROFILES_ACTIVE=demo
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
  CMD ["sh","-c","wget -qO- http://localhost:8080/actuator/health/readiness | grep -q UP"]
ENTRYPOINT ["java","-jar","/app/app.jar"]
