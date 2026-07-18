# ==========================
# Stage 1 - Build the project
# ==========================
FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Download dependencies
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src src

# Build the applicatio
RUN ./mvnw clean package -DskipTests && ls -la target

# ==========================
# Stage 2 - Runtime image
# ==========================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the generated JAR
COPY --from=builder /app/target/*.jar app.jar

# Render provides the PORT environment variable.
ENV PORT=8080

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Dserver.port=$PORT -jar app.jar"]
