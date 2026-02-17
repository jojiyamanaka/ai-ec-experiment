# Phase 3 アクセス制御テスト結果

## テスト実施日
2026-02-16

## テスト環境
- Docker Compose
- macOS (zsh)
- ネットワーク分離設定適用済み
- 注記: ホスト環境から `localhost:3001` / `localhost:3002` への直接到達ができない場合は、
  BFFの疎通確認は `docker compose exec` 経由で実施

## テスト結果

### 1. 外部からCore APIへの直接アクセス

| エンドポイント | 結果 | 期待値 |
|--------------|------|-------|
| GET /products | HTTP=000 (`Couldn't connect to server`) | ✅ 遮断 |
| POST /auth/login | HTTP=000 (`Couldn't connect to server`) | ✅ 遮断 |
| GET /admin/inventory | HTTP=000 (`Couldn't connect to server`) | ✅ 遮断 |
| GET /actuator/health | HTTP=000 (`Couldn't connect to server`) | ✅ 遮断 |

### 2. BFF経由でのアクセス

| BFF | エンドポイント | 結果 | 期待値 |
|-----|--------------|------|-------|
| Customer | GET /api/products | 200 OK | ✅ 成功 |
| Customer | POST /api/cart/items | 201 Created | ✅ 成功 |
| BackOffice | GET /api/inventory | 200 OK | ✅ 成功 |
| BackOffice | PUT /api/inventory/1 | 200 OK | ✅ 成功 |

### 3. 内部ネットワーク疎通

| コンテナ | Core API疎通 | 応答 | 結果 |
|---------|-------------|------|------|
| customer-bff | curl http://backend:8080/actuator/health | `{"status":"UP"}` | ✅ 成功 |
| backoffice-bff | curl http://backend:8080/actuator/health | `{"status":"UP"}` | ✅ 成功 |

## 結論

Core APIの内部化は正常に完了。外部直アクセスは遮断され、BFF経由アクセスは成功。
