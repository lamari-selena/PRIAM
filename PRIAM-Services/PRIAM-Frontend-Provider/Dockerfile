# syntax = docker/dockerfile:1.4.0
FROM node:22 AS builder

# Options — all host-published defaults, routed through the Gateway (not
# individual microservice ports) to match how CORS/auth are configured there.
ARG API_DATA="localhost:8090/data"
ARG API_RIGHT="localhost:8090/right"
ARG API_ACTOR="localhost:8090/actor"
ARG API_PROVIDER="localhost:8090/provider"
ARG API_CONSENT="localhost:8090/cdp"
ARG GATEWAY_ORIGIN="localhost:8090"
# Provider-agnostic OIDC config — see Docs/PRIAM-AUTH-OIDC.md.
ARG OIDC_ISSUER="localhost:8080/realms/priam-realm"
ARG OIDC_CLIENT_ID="Provider-client"

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
        api_const: 'http://${API_CONSENT}/api',
        api_actor: 'http://${API_ACTOR}',
        api_provider: 'http://${API_PROVIDER}',
        gatewayOrigin: 'http://${GATEWAY_ORIGIN}',
        oidcIssuer: 'http://${OIDC_ISSUER}',
        oidcClientId: '${OIDC_CLIENT_ID}'
      }
      " > /app/src/environment/environment.ts
EOF

CMD [ "npm", "start" ]