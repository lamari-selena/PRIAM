# Options
## La version de l'application (le .jar)
ARG APP_VERSION="0.0.1-SNAPSHOT" 
## La version de gradle
ARG GRADLE_VERSION="8.7"
## La version du java-runtime
ARG JRE_VERSION="17"

# Image qui va créer le .jar
FROM gradle:${GRADLE_VERSION} AS builder

WORKDIR /app

# On copie tout ce qu'on a
COPY . /app

# Build le .jar
RUN gradle build

# Image qui va run le code
FROM ubuntu

# Répétition pour cette image
ARG APP_VERSION
ARG JRE_VERSION

EXPOSE 8761
WORKDIR /app

# Update + installation de la JRE-17
RUN apt update && apt upgrade -y && apt install curl openjdk-${JRE_VERSION}-jre -y

# On copie les .jar qu'on a build (todo: prendre auto que la version sans le "-plain")
COPY --from=builder /app/build/libs/Eureka_service-${APP_VERSION}.jar .

# On transforme la ARG en ENV pour qu'on puisse l'utiliser dans l'entrypoint
ENV APP_VERSION_ENV=${APP_VERSION} 
# Lancement du .jar
ENTRYPOINT java -jar Eureka_service-$APP_VERSION_ENV.jar