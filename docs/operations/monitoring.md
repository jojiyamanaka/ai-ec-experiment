# BFF構成 監視項目

## 1. Customer BFF

| 項目 | エンドポイント | 閾値 | アラート |
|-----|--------------|------|---------|
| ヘルスチェック | GET /health | status != "ok" | Critical |
| エラー率 | ログ集計 | > 5% | Warning |
| p95レスポンスタイム | ログ集計 | > 500ms | Warning |
| CPU使用率 | Docker Stats | > 80% | Warning |
| メモリ使用率 | Docker Stats | > 80% | Warning |

## 2. BackOffice BFF

| 項目 | エンドポイント | 閾値 | アラート |
|-----|--------------|------|---------|
| ヘルスチェック | GET /health | status != "ok" | Critical |
| エラー率 | ログ集計 | > 5% | Warning |
| p95レスポンスタイム | ログ集計 | > 800ms | Warning |

## 3. Core API

| 項目 | エンドポイント | 閾値 | アラート |
|-----|--------------|------|---------|
| ヘルスチェック | GET /actuator/health | status != "UP" | Critical |
| DB接続 | /actuator/health/db | DOWN | Critical |
| エラー率 | ログ集計 | > 3% | Warning |

## 4. ネットワーク

| 項目 | 確認方法 | 閾値 | アラート |
|-----|---------|------|---------|
| BFF→Core API疎通 | 内部ヘルスチェック | 失敗 | Critical |
| 外部→Core API遮断 | 定期確認 | アクセス可能 | Critical |

## 5. 監視コマンド例（Docker Compose）

```bash
# BFF ヘルス
docker compose exec -T customer-bff curl -fsS http://localhost:3001/health
docker compose exec -T backoffice-bff curl -fsS http://localhost:3002/health

# BFF から Core API への内部疎通
docker compose exec -T customer-bff curl -fsS http://backend:8080/actuator/health
docker compose exec -T backoffice-bff curl -fsS http://backend:8080/actuator/health

# 外部から Core API 直アクセス不可の確認（失敗が正）
curl -sS -o /dev/null -w "%{http_code}\n" http://localhost:8080/actuator/health
```
