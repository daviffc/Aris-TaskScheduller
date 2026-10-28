
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .

RUN mvn -B -ntp dependency:go-offline

COPY src ./src

RUN mvn -B -ntp -DskipTests clean package

FROM eclipse-temurin:24-jre

ARG BUILD_SHA=local
ENV ARIS_BUILD_SHA=${BUILD_SHA}

RUN groupadd --system aris \
    && useradd --system --gid aris --create-home aris

WORKDIR /app

COPY --from=build --chown=aris:aris \
    /app/target/ArisTaskScheduller-1.0-SNAPSHOT.jar app.jar

USER aris

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
