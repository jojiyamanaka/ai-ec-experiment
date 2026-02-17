# Phase 3 ロールバック手順検証結果

## テスト実施日
2026-02-16

## テスト内容

### 1. ロールバック手順（Core API再公開）

| ステップ | 実施内容 | 所要時間 | 結果 |
|---------|---------|---------|------|
| 1 | `docker-compose.yml` を一時修正（`backend` に `ports` と `public` ネットワーク追加） | 1分 | ✅ |
| 2 | `docker compose up -d backend` で再起動 | 1分 | ✅ |
| 3 | `frontend-customer` から `http://backend:8080/actuator/health` が `{"status":"UP"}` で応答 | 30秒 | ✅ |
| **合計** | | **2分30秒** | **✅** |

### 2. 復旧手順（BFF構成に戻す）

| ステップ | 実施内容 | 所要時間 | 結果 |
|---------|---------|---------|------|
| 1 | `docker-compose.yml` を元に戻す（`backend` 非公開・`internal` のみ） | 1分 | ✅ |
| 2 | `docker compose up -d backend` で再起動 | 1分 | ✅ |
| 3 | `curl http://localhost:8080/actuator/health` が `HTTP=000`（接続不可） | 30秒 | ✅ |
| 4 | `frontend-customer` から `http://backend:8080/actuator/health` が `bad address` | 30秒 | ✅ |
| **合計** | | **3分** | **✅** |

## 結論

ロールバック手順は正常に動作。緊急時に迅速に切り戻し可能。
