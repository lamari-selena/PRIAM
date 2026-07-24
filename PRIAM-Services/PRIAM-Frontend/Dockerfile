# syntax = docker/dockerfile:1.4.0
FROM node:22 AS builder

# Options
# ARG API_DATA="localhost:8081"
# ARG API_RIGHT="localhost:8083"
# ARG API_ACTOR="localhost:8082"
# ARG API_PROVIDER="localhost:8086"
# ARG API_CONSENT="localhost:8089"
# ARG OIDC_ISSUER="localhost:8080/realms/priam-realm"


ARG API_DATA="localhost:8090/data"
ARG API_RIGHT="localhost:8090/right"
ARG API_ACTOR="localhost:8090/actor"
ARG API_PROVIDER="localhost:8090/provider"
ARG API_CONSENT="localhost:8090/cdp"
ARG GATEWAY_ORIGIN="localhost:8090"
# Provider-agnostic: any standards-compliant OIDC issuer works here (Keycloak
# realm URL, Auth0 domain, Okta, a broker, ...). See Docs/PRIAM-AUTH-OIDC.md.
ARG OIDC_ISSUER="localhost:8080/realms/priam-realm"
ARG OIDC_CLIENT_ID="Data-client"
# Generic "back to the app" link for the Home page — empty by default (no
# button shown) since PRIAM itself doesn't know which target app it's
# paired with; each case study's docker-compose.yml sets this to its own
# browser-facing origin (e.g. Ghostfolio's ROOT_URL). Full URL, not just a
# host, since target apps mount at different paths (e.g. Ghostfolio serves
# from /).
ARG TARGET_APP_URL=""
ENV TARGET_APP_URL=${TARGET_APP_URL}

WORKDIR /app

COPY package*.json .

RUN npm i

COPY . .

# Cela permet de réécrire les options d'environnement selon ce qu'on veut.
# Written as individual appends rather than one multi-line RUN <<EOF heredoc:
# the heredoc form silently resolved TARGET_APP_URL to an empty string in the
# generated file - reproduced repeatedly, always only that one variable,
# always correct right up to the heredoc (confirmed via a plain RUN echo
# placed immediately before it), always correct for every other variable
# inside that same heredoc. Root cause not identified (tried ARG->ENV
# promotion above, ruled out COPY/npm i via bisection, ruled out caching with
# --no-cache) - this sidesteps it rather than leaving it unfixed. If a future
# case study's build arg goes missing the same way, suspect the same heredoc
# quirk before anything else.
RUN echo "export const environment = {" > /app/src/environment/environment.ts && \
    echo "  production: true," >> /app/src/environment/environment.ts && \
    echo "  api_data: 'http://${API_DATA}/api'," >> /app/src/environment/environment.ts && \
    echo "  api_right: 'http://${API_RIGHT}/api'," >> /app/src/environment/environment.ts && \
    echo "  api_const: 'http://${API_CONSENT}/api'," >> /app/src/environment/environment.ts && \
    echo "  api_actor: 'http://${API_ACTOR}'," >> /app/src/environment/environment.ts && \
    echo "  api_provider: 'http://${API_PROVIDER}'," >> /app/src/environment/environment.ts && \
    echo "  gatewayOrigin: 'http://${GATEWAY_ORIGIN}'," >> /app/src/environment/environment.ts && \
    echo "  oidcIssuer: 'http://${OIDC_ISSUER}'," >> /app/src/environment/environment.ts && \
    echo "  oidcClientId: '${OIDC_CLIENT_ID}'," >> /app/src/environment/environment.ts && \
    echo "  targetAppUrl: '${TARGET_APP_URL}'" >> /app/src/environment/environment.ts && \
    echo "}" >> /app/src/environment/environment.ts

CMD [ "npm", "start" ]