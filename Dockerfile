# syntax=docker/dockerfile:1
# 킹세종 서버 이미지. 배포는 deploy/ 의 docker compose 로 띄운다 (deploy/README.md).
# 테스트는 CI에서 돌리므로 이미지 빌드에서는 건너뛴다 (Testcontainers는 도커 안에서 못 돎).

FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -q dependencies > /dev/null
COPY src src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon -q bootJar -x test \
    && cp build/libs/*.jar /src/app.jar

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER app
ENV SPRING_PROFILES_ACTIVE=prod \
    TZ=Asia/Seoul \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
