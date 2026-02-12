# ai-ec-experiment

AI を活用した EC サイト実験プロジェクト

## 技術スタック

- **フロントエンド**: React + TypeScript + Vite + Tailwind CSS
- **バックエンド**: Spring Boot + SQLite
- **開発環境**: Docker Compose

## 開発環境のセットアップ

### 前提条件
- Docker Desktop インストール済み

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
- フロントエンド: http://localhost:5173
- バックエンド API: http://localhost:8080

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
