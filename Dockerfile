# Build the JAR first: ./gradlew bootJar
# Then: docker build -t vanilla .
#
# In CI the JAR is pre-built and copied from build/libs/.
# For local use: ./gradlew bootJar && docker build .

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

# curl is required for the compose healthcheck; not present in the base JRE image
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run as non-root
RUN groupadd -r app && useradd -r -g app app

COPY build/libs/*.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 8080

# -Xms32m        : small initial heap; JVM grows to Xmx only as needed
# -Xmx128m       : heap cap (simple CRUD app steady-state is well under 100m)
# -MaxMetaspaceSize: Spring+Hibernate+Springdoc loads ~10k classes; 160m gives a small buffer
# -ReservedCodeCacheSize: cap JIT code cache (default 512m reserved; 64m is ample for this app)
# -Xss512k       : halve default thread-stack size (saves ~10m across Tomcat threads)
# No explicit GC: Java 25 defaults to G1GC which keeps pause times short, essential for
#                 health-check responsiveness. SerialGC caused stop-the-world pauses long
#                 enough to fail curl health checks.
ENTRYPOINT ["java", \
  "-Xms32m", "-Xmx128m", \
  "-XX:MaxMetaspaceSize=160m", \
  "-XX:ReservedCodeCacheSize=64m", \
  "-Xss512k", \
  "-jar", "app.jar"]
