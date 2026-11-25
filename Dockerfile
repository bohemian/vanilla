# Build the JAR first: ./gradlew bootJar
# Then: docker build -t vanilla .
#
# In CI the JAR is pre-built and copied from build/libs/.
# For local use: ./gradlew bootJar && docker build .

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app

# Run as non-root
RUN groupadd -r app && useradd -r -g app app

COPY build/libs/*.jar app.jar
RUN chown app:app app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xms32m", "-Xmx128m", \
  "-XX:MaxMetaspaceSize=160m", \
  "-XX:ReservedCodeCacheSize=64m", \
  "-XX:+UseSerialGC", \
  "-Xss512k", \
  "-jar", "app.jar"]
