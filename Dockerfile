FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy gradle files
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# Copy source code
COPY . .

# Build the application
RUN chmod +x gradlew && ./gradlew bootJar -x test

# Create final image with just the JAR
FROM openjdk:21
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=0 /app/build/libs/*.jar app.jar

EXPOSE 8080

# Add environment variables
ENV OPENAI_API_KEY=${OPENAI_API_KEY}
ENV SPRING_PROFILES_ACTIVE=production

ENTRYPOINT ["java", "-jar", "app.jar"]
