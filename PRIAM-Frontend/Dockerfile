# syntax = docker/dockerfile:1.4.0
FROM node:22 AS builder

# Options
# ARG API_DATA="localhost:8081"
# ARG API_RIGHT="localhost:8083"
# ARG API_ACTOR="localhost:8082"
# ARG API_PROVIDER="localhost:8086"
# ARG API_CONSENT="localhost:8089"
# ARG KEYCLOAK="localhost:8080"


ARG API_DATA="localhost:8090/data"
ARG API_RIGHT="localhost:8090/right"
ARG API_ACTOR="localhost:8090/actor"
ARG API_PROVIDER="localhost:8090/provider"
ARG API_CONSENT="localhost:8090/cdp"
ARG KEYCLOAK="localhost:8080"

WORKDIR /app

COPY package*.json .

RUN npm i

COPY . .

# Cela permet de réécrire les options d'environnement selon ce qu'on veut
RUN <<EOF
    echo "export const environment = {
        production: true,
        api_data: 'http://${API_DATA}/api',
        api_right: 'http://${API_RIGHT}/api',
        api_consent: 'http://${API_CONSENT}/api',
        api_actor: 'http://${API_ACTOR}',
        api_provider: 'http://${API_PROVIDER}',
        keycloak: 'http://${KEYCLOAK}'
      }
      " > /app/src/environment/environment.ts
EOF

CMD [ "npm", "start" ]