# syntax = docker/dockerfile:1.4.0
# Options
## La version de l'application (le .jar)
ARG APP_VERSION="0.0.1-SNAPSHOT" 
## La version de gradle
ARG GRADLE_VERSION="8.7"
## La version du java-runtime
ARG JRE_VERSION="17"
## Pour les feignClient
ARG DATA_SERVICE_URL="http://localhost:8081"
ARG ACTOR_SERVICE_URL="http://localhost:8082"
ARG PROVIDER_SERVICE_URL="http://localhost"


# Image qui va créer le .jar
FROM gradle:${GRADLE_VERSION} AS builder

WORKDIR /app

# On copie tout ce qu'on a
COPY . /app

RUN <<EOF
    echo "
    package priam.right.openfeign;

    public class Environment { 
        protected static final String data_url = \"${DATA_SERVICE_URL}\";
        protected static final String actor_url = \"${ACTOR_SERVICE_URL}\";
        protected static final String provider_service_url = \"${PROVIDER_SERVICE_URL}\";
    }
      " > /app/src/main/java/priam/right/openfeign/Environment.java
EOF

# Build le .jar
RUN gradle assemble

# Image qui va run le code
FROM ubuntu

# Répétition pour cette image
ARG APP_VERSION
ARG JRE_VERSION

EXPOSE 8083
WORKDIR /app

# Update + installation de la JRE-17
RUN apt update && apt upgrade -y && apt install curl openjdk-${JRE_VERSION}-jre -y

# On copie les .jar qu'on a build (todo: prendre auto que la version sans le "-plain")
COPY --from=builder /app/build/libs/PRIAM-Right-service-${APP_VERSION}.jar .

# On transforme la ARG en ENV pour qu'on puisse l'utiliser dans l'entrypoint
ENV APP_VERSION_ENV=${APP_VERSION} 
# Lancement du .jar
ENTRYPOINT java -jar PRIAM-Right-service-$APP_VERSION_ENV.jar