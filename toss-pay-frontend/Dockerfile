FROM node:18-alpine

WORKDIR /app
RUN corepack enable && corepack prepare yarn@stable --activate

COPY package.json yarn.lock ./
RUN yarn install --network-timeout 600000

COPY . .

CMD ["yarn", "dev"]
