FROM node:18

WORKDIR /app
COPY package.json package-lock.json ./
RUN npm config set registry https://registry.npmjs.org/ && \
    npm install --legacy-peer-deps --network-timeout=600000

COPY . .

CMD ["npm", "run", "dev"]
