# Spring Boot 애플리케이션을 위한 Dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build

# 작업 디렉토리 설정
WORKDIR /app

# pom.xml 복사 (캐시 최적화)
COPY pom.xml .

# 의존성 다운로드 (레이어 캐싱)
RUN mvn dependency:go-offline -B || true

# 소스 코드 복사
COPY src ./src

# 애플리케이션 빌드
RUN mvn clean package -DskipTests -B

# 런타임 이미지
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/target/*.jar app.jar

# 데이터 디렉토리 생성 (H2 데이터베이스 파일용)
RUN mkdir -p /app/data

# 포트 노출
EXPOSE 8080

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

