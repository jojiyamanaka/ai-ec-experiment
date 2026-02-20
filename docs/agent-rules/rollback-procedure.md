# BFF構成ロールバック手順

## 概要
Phase 3完了後、緊急時にCore APIを直接公開する手順。

## 前提条件
- Phase 1/2/3が完了済み
- Core APIがinternal networkのみに配置されている
- BFF経由でのみAPI利用可能

## ロールバック手順

### Step 1: docker-compose.yml を一時修正

```yaml
# backend サービスにポート公開を追加
backend:
  ports:
    - "8080:8080"  # 再公開
  networks:
    - public     # 追加
    - internal
```

### Step 2: backendコンテナ再起動

```bash
docker compose up -d backend
```

### Step 3: 疎通確認

```bash
curl http://localhost:8080/actuator/health
# → 正常に応答すればOK
```

### Step 4: フロントエンド切り戻し（オプション）

BFF障害時のみ実施（要注意）：

- 現行フロントエンドは BFF 契約（例: `/api/products`）を前提としている
- Core API 直結（`localhost:8080`）はエンドポイント差異（例: `/api/item`）により、そのままでは動作しない可能性が高い
- 実施する場合は、互換ルーティング（リバースプロキシ等）を同時に用意する

```bash
# frontend/.env.customer
VITE_API_URL=http://localhost:8080

# frontend/.env.admin
VITE_API_URL=http://localhost:8080
```

```bash
# フロントエンド再ビルド・デプロイ
cd frontend
npm run build
docker compose up -d frontend-customer frontend-admin
```

### Step 5: 動作確認

- [ ] ブラウザで商品一覧が表示される
- [ ] カート追加・注文が正常動作する
- [ ] 管理画面が正常動作する
- [ ] UI から呼び出す API パスが Core API で解決できる（互換ルーティング含む）

### Step 6: 監視強化

ロールバック期間中は以下を監視：
- Core APIのエラー率
- レスポンスタイム
- CPU/メモリ使用率

## ロールバック後の復旧手順

問題解決後、BFF構成に戻す：

1. docker-compose.ymlを元に戻す（backendポート非公開）
2. backend再起動
3. フロントエンド設定を元に戻す（BFF URL）
4. 疎通確認

## 所要時間

- ロールバック: 約5分
- 復旧: 約10分
