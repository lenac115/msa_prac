FROM node:18-alpine

WORKDIR /app

# 의존성 설치
COPY package.json package-lock.json ./
RUN npm config set registry https://registry.npmjs.org/ && \
    npm install --legacy-peer-deps --network-timeout=600000

# 앱 전체 복사
COPY . .

# 빌드
RUN npm run build

CMD ["npm", "run", "start"]