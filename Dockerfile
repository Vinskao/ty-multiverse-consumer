# Start with a base image that has Java (using Eclipse Temurin for JDK 21)
FROM eclipse-temurin:21-jre

# Set the working directory
WORKDIR /app

# Copy the application's JAR and dependencies
COPY target/ty-multiverse-consumer.jar /app/ty-multiverse-consumer.jar

# Set the working directory
WORKDIR /app

# Expose the application's port
EXPOSE 8081

# Set the environment variable for Spring profile
ENV SPRING_PROFILES_ACTIVE=platform

# Set JVM memory options
ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication"

# Run the application with the specified profile and JVM options
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/ty-multiverse-consumer.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
