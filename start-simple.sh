#!/bin/bash

# Set Java environment for Windows Java in WSL
export JAVA_HOME="/mnt/c/jdk-17.0.12"
export PATH="/mnt/c/jdk-17.0.12/bin:$PATH"

# Convert Windows paths for Maven
export MAVEN_OPTS="-Djava.awt.headless=true"

echo "Java version:"
java.exe -version

echo "Starting application in debug mode..."
# Use java.exe directly instead of relying on mvnw wrapper
java.exe -jar target/wallet-system-1.0.0.jar --spring.profiles.active=dev --debug=true || ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev -Ddebug=true