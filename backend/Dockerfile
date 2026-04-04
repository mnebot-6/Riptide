# Multi-stage build for Riptide Backend
# Supports both: docker build from /backend, and Railway (context = repo root)
FROM gradle:8.12-jdk17 AS build
WORKDIR /app
COPY backend/build.gradle.kts backend/settings.gradle.kts backend/gradle.properties ./
COPY backend/src ./src
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*-all.jar app.jar
EXPOSE ${PORT:-8080}
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
