FROM node:18-alpine as builder

WORKDIR /app
COPY . .
RUN yarn install
RUN yarn build
RUN yarn export

FROM nginx:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=builder /app/out /usr/share/nginx/html
