# --- Build stage ---
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src src
RUN ./mvnw -B -DskipTests package

# Application Insights Java agent: attached unconditionally via JAVA_TOOL_OPTIONS below, but it
# self-disables gracefully (logs a warning, no telemetry, no crash) when
# APPLICATIONINSIGHTS_CONNECTION_STRING isn't set — safe for local/CI runs with no App Insights resource.
ARG APPLICATIONINSIGHTS_AGENT_VERSION=3.7.9
RUN wget -q -O applicationinsights-agent.jar \
    "https://repo1.maven.org/maven2/com/microsoft/azure/applicationinsights-agent/${APPLICATIONINSIGHTS_AGENT_VERSION}/applicationinsights-agent-${APPLICATIONINSIGHTS_AGENT_VERSION}.jar"

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=build /workspace/target/*.jar app.jar
COPY --from=build /workspace/applicationinsights-agent.jar agent/applicationinsights-agent.jar

ENV JAVA_TOOL_OPTIONS="-javaagent:/app/agent/applicationinsights-agent.jar"

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
