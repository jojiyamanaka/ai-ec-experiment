# 環境設定

## PostgreSQL ポート公開制御

**開発環境（デフォルト）**: ホストから直接接続可能（5432）

**本番環境（推奨）**: 環境変数で無効化
```bash
POSTGRES_HOST_PORT=0  # .env または docker compose 実行時に指定
```

**確認方法**:
```bash
docker compose ps postgres  # 5432:5432 表示 = 公開中
```

**理由**: 本番環境では internal network のみアクセス許可し、外部露出を防ぐ

## Core API

内部ネットワーク化済み（8080 非公開）。緊急時の一時公開手順は `rollback-procedure.md` 参照。
