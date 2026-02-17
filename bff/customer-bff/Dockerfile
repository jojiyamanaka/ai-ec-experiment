# syntax=docker/dockerfile:1.7

FROM node:20-alpine AS builder
WORKDIR /app

# shared の依存関係も含めてインストール
COPY package.json package-lock.json ./
COPY bff/shared/package.json ./bff/shared/package.json
COPY bff/customer-bff/package.json ./bff/customer-bff/package.json

RUN --mount=type=cache,target=/root/.npm \
    npm ci --workspace=bff/shared --workspace=bff/customer-bff --include-workspace-root=false

# 先に shared をビルドして、customer-bff 側の変更時にキャッシュを活かす
COPY bff/shared ./bff/shared

WORKDIR /app/bff/shared
RUN npm run build

COPY bff/customer-bff /app/bff/customer-bff

WORKDIR /app/bff/customer-bff
RUN npm run build

# 本番イメージ
FROM node:20-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /app/bff/customer-bff/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/bff/customer-bff/package.json ./

EXPOSE 3001

CMD ["node", "dist/main.js"]
