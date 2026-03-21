# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .
COPY src src

RUN chmod +x mvnw && ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

ENV TZ=Asia/Shanghai
ENV SCIPTV_HTTP_PROXY_BASE_URL=http://192.168.3.1:8188
ENV SCIPTV_EPG_URLS=https://epg.51zmt.top:8001/e.xml,https://epg.112114.xyz/pp.xml
ENV SCIPTV_FCC_ADDRESS=182.139.234.40:8027

COPY --from=builder /app/target/sciptv-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
