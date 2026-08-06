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
# uploads-data 볼륨이 마운트될 경로. 이미지에 먼저 만들어 소유권을 넘겨둬야
# named volume이 최초 생성될 때 이 소유권/권한을 그대로 물려받아 pallang 사용자가 쓸 수 있다.
RUN mkdir -p uploads && chown -R pallang:pallang app.jar uploads
USER pallang

ENV SPRING_PROFILES_ACTIVE=prod
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080' || exit 1

# docker-compose의 TZ=Asia/Seoul과 별개로, LocalDateTime.now()가 쓰는 JVM 기본 시간대를
# 컨테이너 tzdata/TZ 환경변수 설정 여부와 무관하게 명시적으로 고정한다.
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "/app/app.jar"]
