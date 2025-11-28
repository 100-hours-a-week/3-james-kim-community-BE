## Build Stage
#FROM eclipse-temurin:21-jdk-alpine AS builder
#
## Work Directory
#WORKDIR /app
#
## Layer1: Gradle Wrapper 복사
#COPY gradlew ./
#COPY gradle gradle
#
## Layer2: Grdale 빌드 설정 파일 복사
#COPY build.gradle settings.gradle ./
#
## Layer3: 의존성 다운로드
#RUN ./gradlew dependencies --no-daemon
#
## Layer4: 소스 코드 복사
#COPY src ./src
#
## Layer5: 빌드 (JAR 생성)
#RUN ./gradlew bootJar -x test --no-daemon
#
## ======================================

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine

# Work Directory
WORKDIR /app

# 빌드 스테이지에서 JAR 파일만 가져다 씀
COPY build/libs/app.jar app.jar

# 포트 노출 명시
EXPOSE 8080

# 컨테이너 실행
CMD ["java", "-jar", "app.jar"]