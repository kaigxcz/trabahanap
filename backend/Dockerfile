FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./
RUN ./mvnw dependency:go-offline -q
COPY src ./src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/jobconnect-backend-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
