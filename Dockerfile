
FROM maven:3.9-eclipse-temurin-26 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn -B -ntp dependency:go-offline

COPY src ./src

RUN mvn -B -ntp -DskipTests clean package

FROM eclipse-temurin:21-jre

ARG BUILD_SHA=local
ENV ARIS_BUILD_SHA=${BUILD_SHA}

WORKDIR /app

COPY --from=build /app/target/ArisTaskScheduller-1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
