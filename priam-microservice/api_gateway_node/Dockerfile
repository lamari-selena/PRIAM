# This is the dockerfile for dev
# before merge-request to recette|prod:
#   - cp this to Dockerfile-dev
#   - cp Dockerfile-rec+prod to this

FROM node:18

RUN apt-get update && apt-get upgrade -y && apt-get install -y python3 python3-venv python3-pip

RUN python3 -m venv /venv
RUN /venv/bin/pip install psycopg2-binary

ENV PATH="/venv/bin:$PATH"
ENV NODE_NO_BUFFERING=1

RUN apt-get clean && rm -rf /var/lib/apt/lists/*

# Create app directory and default directory to put things
# (This way we do not have to type out full file paths but
#   can use relative paths based on the working directory. )
RUN mkdir -p /usr/src/app

WORKDIR /usr/src/app

# Install app dependencies
# (this way we don’t rebuild node modules each time we re-build the container ;
#   if package.json changes then node modules will be rebuilt )
# A wildcard is used to ensure both package.json AND package-lock.json are copied
# where available (npm@5+)
COPY package*.json ./

RUN npm install

# To use rabbitMq to publish

# If you are building your code for production
# RUN npm ci --only=production

# Bundle app source
COPY . .

USER node

EXPOSE 8080

# Command to run the app when the container starts
CMD [ "npm", "start" ]
