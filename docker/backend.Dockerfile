# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY simulacro_nombreAlumno/pom.xml ./
COPY simulacro_nombreAlumno/src/ ./src/
RUN mvn -B -ntp -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S gym && adduser -S -G gym gym
WORKDIR /app
COPY --from=build --chown=gym:gym /app/target/simulacro_nombreAlumno-0.0.1-SNAPSHOT.jar ./app.jar
USER gym
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=10 CMD wget -q -O /dev/null http://127.0.0.1:8080/v3/api-docs || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
