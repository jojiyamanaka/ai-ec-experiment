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
| PostgreSQL外部公開 | docker ps | 本番でポート公開 | Warning |
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

---

## 6. OTel 観測スタック

| ツール | URL | 用途 |
|--------|-----|------|
| Jaeger | http://localhost:16686 | トレース可視化（フロント〜BFF〜CoreAPI〜DB の一気通貫） |
| Prometheus | http://localhost:9090 | メトリクス収集・確認 |
| Grafana | http://localhost:3000 | ダッシュボード（ビジネスメトリクス含む） |

### トレース構成

- Browser → OTel Collector (OTLP/HTTP `:4318`)
- NestJS BFF / Spring Boot → OTel Collector (OTLP/gRPC `:4317`)
- W3C `traceparent` ヘッダーでサービス間伝搬

### メトリクス収集経路

- **NestJS BFF**: OTLP → Collector → Prometheus pull (`:8889`)
- **Spring Boot**: `/actuator/prometheus` を Prometheus が直接スクレイプ

### ビジネスメトリクス

| メトリクス | 種別 | 実装箇所 |
|-----------|------|---------|
| 注文成功/失敗 | Counter | `OrderUseCase` |
| 在庫引当失敗 | Counter | `OrderUseCase` |
| 認証失敗 | Counter | `AuthService` |
| 注文処理時間 | Timer | `OrderUseCase` |
