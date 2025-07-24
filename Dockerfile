
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY banking-app/pom.xml .
COPY banking-app/src ./src
COPY banking-app/mvnw .
COPY banking-app/.mvn .mvn
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
