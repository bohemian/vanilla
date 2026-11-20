# Build the JAR first: ./gradlew bootJar
# Then: docker build -t vanilla .
#
# In CI the JAR is pre-built and copied from build/libs/.
# For local use: ./gradlew bootJar && docker build .

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

# Upgrade OS packages at build time to apply all available security patches.
# This eliminates fixable CVEs before the Trivy scan runs in CI.
RUN apt-get update && apt-get upgrade -y && rm -rf /var/lib/apt/lists/*

# Run as non-root
RUN groupadd -r app && useradd -r -g app app

COPY build/libs/*.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
