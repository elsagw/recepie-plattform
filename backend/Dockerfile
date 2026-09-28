# Stage 1: build the jar with the full JDK + Maven wrapper.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copy only the files Maven needs to resolve dependencies first, so this layer
# (the slow part) is cached and skipped on rebuilds that only change source code.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -DskipTests package

# Stage 2: the actual runtime image - JRE only, no compiler/build tools.
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
