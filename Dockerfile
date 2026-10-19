# Build stage: compile and package with the Maven wrapper
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# Runtime stage: a small JRE image running as a non-root user
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 stockroom
COPY --from=build /app/target/stockroom-api-*.jar app.jar
USER stockroom
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
