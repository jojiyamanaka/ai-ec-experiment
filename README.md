# ai-ec-experiment

AI を活用した EC サイト実験プロジェクト

## 技術スタック

- **フロントエンド**: React + TypeScript + Vite + Tailwind CSS
- **バックエンド**: Spring Boot + PostgreSQL
- **開発環境**: Docker Compose

## 開発環境のセットアップ

### 前提条件
- Docker Desktop インストール済み
- Node.js 20.x / npm 10.x（ローカルで `npm run build:all` を実行する場合）
- Java 21 Runtime（ローカルで `backend` を直接起動する場合）

### 起動方法

```bash
# すべてのコンテナを起動
docker-compose up

# バックグラウンドで起動
docker-compose up -d

# 特定のサービスのみ起動
docker-compose up frontend
docker-compose up backend
```

### アクセス
- 顧客画面: http://localhost:5173
- 管理画面: http://localhost:5174
- Customer BFF: http://localhost:3001
- BackOffice BFF: http://localhost:3002
- Core API: 直接アクセス不可（内部ネットワークのみ）

### 停止方法
```bash
# コンテナを停止
docker-compose down

# コンテナ＋ボリュームを削除
docker-compose down -v
```

### ログ確認
```bash
# すべてのログ
docker-compose logs -f

# 特定のサービスのログ
docker-compose logs -f frontend
docker-compose logs -f backend
```

## ディレクトリ構成

```
.
├── frontend/          # React アプリ
├── backend/           # Spring Boot アプリ
├── docs/              # 仕様書
├── docker-compose.yml # Docker 構成
└── README.md
```

## 開発フロー

1. コードを編集（`frontend/` または `backend/`）
2. 変更が自動的にコンテナ内に反映される（ホットリロード）
3. ブラウザで確認

## 詳細ドキュメント

- [仕様書](./docs/SPEC.md)
- [API仕様](./docs/api-spec.md)
- [テスト実施ガイド](./docs/test/testing-operations.md)
- [Playwright 利用ガイド](./docs/test/playwright-runbook.md)

## アーキテクチャ

### システム構成（Phase 3完了後）

```
┌─────────────┐
│  ブラウザ   │
└──────┬──────┘
       │
   ┌───┴───────────┐
   │               │
   ↓               ↓
┌──────────────┐ ┌──────────────┐
│Customer Front│ │ Admin Front  │
│ (Port 5173)  │ │ (Port 5174)  │
└──────┬───────┘ └──────┬───────┘
       ↓                ↓
┌──────────────┐ ┌──────────────┐
│Customer BFF  │ │BackOffice BFF│
│ (Port 3001)  │ │ (Port 3002)  │
└──────┬───────┘ └──────┬───────┘
       └───────┬────────┘
               ↓
                  ┌────────────────┐
                  │   Core API     │
                  │  (内部のみ)    │
                  │  (Port 8080)   │
                  └────────┬───────┘
                           ↓
                  ┌────────────────┐
                  │  PostgreSQL    │
                  └────────────────┘
```

### セキュリティ

- **Core APIは内部ネットワークのみ**: 外部から直接アクセス不可
- **BFF経由のみアクセス可**: Customer BFF / BackOffice BFFを経由
- **認証トークン分離**: User（顧客）とBoUser（管理者）を厳密に分離

## 開発コマンド

### Docker（メインの開発フロー）

```bash
docker compose up -d          # 全コンテナ起動
docker compose down            # 全コンテナ停止
docker compose logs -f         # 全ログ確認
docker compose logs -f customer-bff    # Customer BFFログ
docker compose logs -f backoffice-bff  # BackOffice BFFログ
docker compose logs -f backend         # Core APIログ
```

### アクセスURL

- 顧客画面: http://localhost:5173
- 管理画面: http://localhost:5174
- Customer BFF: http://localhost:3001
- BackOffice BFF: http://localhost:3002
- Core API: 直接アクセス不可（内部ネットワークのみ）
