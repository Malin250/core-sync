# ────────────────────────────────────────────────────────────────
#  Stage 1 — Build
#  Uses a Gradle + JDK image to compile and package the application.
# ────────────────────────────────────────────────────────────────
FROM gradle:8.7-jdk21 AS builder

WORKDIR /app

# Copy dependency manifests first to exploit Docker layer caching.
# The dependency download layer is only rebuilt when build.gradle or
# settings.gradle changes.
COPY settings.gradle build.gradle ./
RUN gradle dependencies --no-daemon --quiet || true

# Copy source and build the fat JAR.
COPY src ./src
RUN gradle bootJar --no-daemon -x test

# ────────────────────────────────────────────────────────────────
#  Stage 2 — Runtime
#  Minimal JRE image — no build tools in the final image.
# ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Run as a non-root user for security.
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

# Pass JVM flags for containerised environments.
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
