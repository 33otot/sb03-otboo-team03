
# -------------------------------------------------------------------
# 1단계: 빌드 스테이지 (빌드 도구 포함)
FROM eclipse-temurin:17-jdk-alpine AS build

# 작업 디렉토리 설정
WORKDIR /app

# Gradle 캐시 최적화: 래퍼/설정만 먼저 복사
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle/ gradle/

# 권한 및 의존성 프리페치(소스 없이도 가능한 범위까지)
RUN chmod +x ./gradlew \
    && ./gradlew --no-daemon dependencies || true

# 소스 복사 후 빌드 (테스트 제외)
COPY src/ src/
RUN ./gradlew --no-daemon clean bootJar -x test


# -------------------------------------------------------------------
# 2단계: 런타임 스테이지 (경량 이미지)
FROM eclipse-temurin:17-jre-alpine AS runtime

# curl 설치
RUN apk add --no-cache curl

# 비루트 유저 생성 및 전환
RUN addgroup -S app && adduser -S app -G app
USER app

# 작업 디렉토리 설정
WORKDIR /app

# 로그 디렉터리 생성
RUN mkdir -p /app/logs

# 빌드된 JAR 복사 및 권한 설정
COPY --from=build --chown=app:app /app/build/libs/*-SNAPSHOT.jar /app/app.jar

# Logback 에서 사용할 로그 디렉터리
ENV LOG_DIR=/app/logs

# 기본 포트 노출 (실제 포트는 SERVER_PORT 환경변수로)
EXPOSE 8080

# 애플리케이션 실행을 위한 ENTRYPOINT와 CMD 조합
CMD ["sh", "-c", "java $JVM_OPTS -jar app.jar --server.port=${SERVER_PORT:-8080} --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
