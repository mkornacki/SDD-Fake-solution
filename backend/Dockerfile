FROM maven:3.9.11-eclipse-temurin-11 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY checkstyle.xml ./
COPY .mvn ./.mvn
COPY mvnw ./
COPY src ./src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:11-jre
WORKDIR /app
RUN mkdir -p /app/data
COPY --from=build /workspace/target/*.jar /app/app.jar
EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
