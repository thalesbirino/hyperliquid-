# ============================================
# Hyperliquid Trading Bot - Multi-stage Dockerfile
# ============================================
# Builds Java application and includes Python SDK

# ==========================================
# Stage 1: Build
# ==========================================
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml first for dependency caching
COPY pom.xml .

# Download dependencies (cached if pom.xml unchanged)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src
COPY scripts ./scripts

# Build application (skip tests for faster builds)
RUN mvn clean package -DskipTests -B

# ==========================================
# Stage 2: Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install Python 3 and pip
RUN apk add --no-cache \
    python3 \
    py3-pip \
    && pip3 install --no-cache-dir --break-system-packages \
    hyperliquid-python-sdk \
    eth-account

# Create non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy application JAR
COPY --from=build /app/target/*.jar app.jar

# Copy Python scripts
COPY --from=build /app/scripts ./scripts

# Set ownership
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Environment variables (override in docker-compose or runtime)
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Entry point
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
