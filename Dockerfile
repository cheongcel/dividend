# 1. 빌드하는 단계 (Gradle로 요리하기)
FROM gradle:8.5-jdk17-alpine as builder
WORKDIR /build

# 소스 코드 복사
COPY build.gradle settings.gradle /build/
COPY src /build/src

# 빌드 실행 (jar 파일 생성)
RUN gradle build --no-daemon -x test

# 2. 실행하는 단계 (가볍게 서빙하기)
# [수정됨] 기존 openjdk 이미지가 없어져서, 대체 이미지(eclipse-temurin)로 변경!
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# 위에서 만든 jar 파일을 가져옴
COPY --from=builder /build/build/libs/*.jar app.jar

# 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]