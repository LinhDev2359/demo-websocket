#!/bin/bash

# Set Java environment for Windows Java in WSL
export JAVA_HOME="/mnt/c/jdk-17.0.12"
export PATH="/mnt/c/jdk-17.0.12/bin:$PATH"

# Convert Windows paths for Maven
export MAVEN_OPTS="-Djava.awt.headless=true"

echo "Java version:"
java.exe -version

echo "Compiling project..."
./mvnw clean compile