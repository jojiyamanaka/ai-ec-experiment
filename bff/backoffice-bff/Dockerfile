# syntax=docker/dockerfile:1.7

FROM node:20-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
COPY bff/shared/package.json ./bff/shared/package.json
COPY bff/backoffice-bff/package.json ./bff/backoffice-bff/package.json

RUN --mount=type=cache,target=/root/.npm \
    npm ci --workspace=bff/shared --workspace=bff/backoffice-bff --include-workspace-root=false

COPY bff/shared ./bff/shared

WORKDIR /app/bff/shared
RUN npm run build

COPY bff/backoffice-bff /app/bff/backoffice-bff

WORKDIR /app/bff/backoffice-bff
RUN npm run build

FROM node:20-alpine
WORKDIR /app

RUN apk add --no-cache curl

COPY --from=builder /app/bff/backoffice-bff/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/bff/backoffice-bff/package.json ./

EXPOSE 3002

CMD ["node", "dist/main.js"]
