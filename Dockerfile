FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy gradle files
COPY core-sync-service/gradlew .
COPY core-sync-service/gradle gradle
COPY core-sync-service/build.gradle .
COPY core-sync-service/settings.gradle .

# Don't forget to update the path for your source code too!
COPY core-sync-service/src src

# Build the application
RUN chmod +x gradlew && ./gradlew bootJar -x test

# Create final image with just the JAR
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=0 /app/build/libs/*.jar app.jar

EXPOSE 8080

# Add environment variables
ENV OPENAI_API_KEY=${OPENAI_API_KEY}
ENV SPRING_PROFILES_ACTIVE=production

ENTRYPOINT ["java", "-jar", "app.jar"]
