FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw package -DskipTests -q
EXPOSE 8080
CMD ["java", "-jar", "target/jobconnect-backend-1.0.0.jar", "--spring.profiles.active=prod"]
