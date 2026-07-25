# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /workspace

# Cache Gradle wrapper/dependencies in their own layer before copying source.
COPY gradlew ./
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ---- Run stage ----
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN addgroup --system pallang && adduser --system --ingroup pallang pallang
COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown pallang:pallang app.jar
USER pallang

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
