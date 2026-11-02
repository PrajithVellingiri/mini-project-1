# =============================================================================
#  Multi-Stage Dockerfile for Spring Boot 4.1.1 on Java 21
#  Optimized for Free-Tier Cloud Hosting (512MB RAM Constraints)
# =============================================================================

# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy maven wrapper and pom.xml first to leverage Docker layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy application source code and compile production JAR
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# ── Stage 2: Production JRE Runtime ───────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-privileged application user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy compiled JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Dynamic port binding and JVM memory optimization for 512MB free-tier RAM limits:
# - Serial Garbage Collector minimizes memory footprint and GC pause overhead
# - Max heap set to 256MB, leaving remaining container RAM for JVM metaspace/OS
ENV PORT=8080 \
    JAVA_OPTS="-XX:+UseSerialGC -Xmx256m -Xms64m -XX:+TieredCompilation -XX:TieredStopAtLevel=1"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar app.jar"]
