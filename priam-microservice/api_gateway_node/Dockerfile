ARG NODE_VERSION="18"

# Image that will compile the TypeScript sources
FROM node:${NODE_VERSION} AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY tsconfig.json ./
COPY src ./src
RUN npm run build

# Image that will run the compiled code
FROM node:${NODE_VERSION}-slim
WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev

COPY --from=builder /app/dist ./dist

USER node

EXPOSE 8090

CMD ["npm", "start"]
