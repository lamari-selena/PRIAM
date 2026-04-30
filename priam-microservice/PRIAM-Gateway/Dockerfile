# Options
## The version of the application (the .jar)
ARG APP_VERSION="0.0.1-SNAPSHOT"
## The version of Gradle
ARG GRADLE_VERSION="7.4"
## The version of the Java runtime
ARG JRE_VERSION="17"

# Image that will create the .jar
FROM gradle:${GRADLE_VERSION}-jdk${JRE_VERSION} AS builder
WORKDIR /app

# Copy everything
COPY . /app

# Build the .jar
RUN ./gradlew assemble

# Image that will run the code
FROM ubuntu:22.04

# Repeat the arguments for this image
ARG APP_VERSION
ARG JRE_VERSION

EXPOSE 8761
WORKDIR /app

# Update + installation of the JRE
RUN apt-get update && \
    apt-get upgrade -y && \
    apt-get install -y curl openjdk-${JRE_VERSION}-jre && \
    rm -rf /var/lib/apt/lists/*

# Copy the .jar that was built
COPY --from=builder /app/build/libs/gateway-0.0.1-SNAPSHOT.jar .

# Transform the ARG to ENV so it can be used in the entrypoint
ENV APP_VERSION_ENV=${APP_VERSION}

# Launch the .jar
ENTRYPOINT ["java", "-jar", "gateway-0.0.1-SNAPSHOT.jar"]
