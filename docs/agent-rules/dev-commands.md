# 開発コマンド

## Docker

```bash
docker compose up -d
docker compose down
docker compose logs -f backend
```

## Frontend（`cd frontend`）

```bash
npm run dev:customer
npm run dev:admin
npm run build
npm run lint
```

## Backend（`cd backend`）

```bash
./mvnw test
./mvnw test -Dtest=ClassName#methodName
./mvnw compile
```

## アクセスURL

- 顧客画面: http://localhost:5173
- 管理画面: http://localhost:5174
- Customer BFF: http://localhost:3001
- BackOffice BFF: http://localhost:3002
- Core API: `backend:8080`（内部ネットワークのみ）
