# ai-ec-experiment

AI推薦機能を備えた EC サイトのプロトタイプ。

## クイックスタート

```bash
docker compose up -d
```

## アクセス URL

| サービス | URL |
|---|---|
| 顧客画面 | http://localhost:5173 |
| 管理画面 | http://localhost:5174 |
| Jaeger UI（トレース） | http://localhost:16686 |
| Grafana（メトリクス） | http://localhost:3000 |
| Prometheus | http://localhost:9090 |

## ドキュメント

- [技術仕様書](./docs/SPEC.md) — アーキテクチャ・インフラ構成・設計方針
- [業務要件](./docs/requirements.md) — ビジネスルール・機能仕様
- [データモデル](./docs/data-model.md) — エンティティ定義・DB スキーマ
- [API 仕様](./docs/api/) — OpenAPI JSON（Customer BFF / BackOffice BFF / Core API）
- [開発ガイド](./CLAUDE.md) — コーディング規約・開発コマンド・機能開発プロセス
