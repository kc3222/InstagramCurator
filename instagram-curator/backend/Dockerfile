# syntax=docker/dockerfile:1.7

# ---------- Build stage ----------
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

# Copy Gradle wrapper + build scripts first so we can cache dependency resolution.
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --version

# Warm the dependency cache. `|| true` so this layer still caches if no deps resolve yet.
RUN ./gradlew dependencies --no-daemon || true

# Now copy source and build.
COPY src src
RUN ./gradlew bootJar --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

# Non-root user for the runtime.
RUN groupadd --system app && useradd --system --gid app --home /app app
USER app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

# Railway injects PORT; locally Spring Boot falls back to 8080 (see application.properties).
ENV JAVA_OPTS=""
EXPOSE 8080

# Healthcheck targets /health so Docker/Railway can verify the app is alive.
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
	CMD wget --quiet --tries=1 --spider "http://localhost:${PORT:-8080}/health" || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
