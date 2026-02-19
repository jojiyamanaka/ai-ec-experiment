# CLAUDE.md

## プロジェクト概要

AIを活用したECサイト実験プロジェクト。React + TypeScript フロントエンド、NestJS BFF、Spring Boot Core API を Docker で動作させる。

## 開発コマンド

```bash
# Docker（メインの開発フロー）
docker compose up -d          # 全コンテナ起動
docker compose down            # 停止
docker compose logs -f backend # 特定サービスのログ確認

# フロントエンド（cd frontend）
npm run dev:customer  # 顧客画面（:5173）
npm run dev:admin     # 管理画面（:5174）
npm run build         # 型チェック + ビルド
npm run lint

# バックエンド（cd backend）
./mvnw test                               # 全テスト
./mvnw test -Dtest=ClassName#methodName   # 特定テスト
./mvnw compile                            # コンパイルのみ
```

### アクセスURL
- 顧客画面: http://localhost:5173 → Customer BFF: http://localhost:3001
- 管理画面: http://localhost:5174 → BackOffice BFF: http://localhost:3002
- Core API: `backend:8080`（内部ネットワークのみ）

## コーディング規約

- **コミットメッセージ**: 日本語で記述
- **フロントエンドAPI呼び出し**: IMPORTANT: 必ず `frontend/src/lib/api.ts` の関数を使用。`fetch` を直接使わないこと
- **フロントエンド型定義**: `frontend/src/types/api.ts` に定義
- **バックエンドDTO**: エンティティと分離し、`fromEntity()` 静的メソッドで変換
- **バックエンド例外**: `ResourceNotFoundException` / `BusinessException` / `ConflictException` をスロー

## 機能開発プロセス

新機能は `CHG-XXX_<機能名>.md` の命名規則で以下3ステップのドキュメントを作成する:

1. **要件定義** (`docs/01_requirements/`) — Why・What のみ。技術的 How は書かない
2. **技術設計** (`docs/02_designs/`) — API設計、実装方針、処理フロー
3. **実装タスク** (`docs/03_tasks/`) — 作業単位への分割・触る範囲（クラス名レベル）・Done条件・検証コマンドを明記。コード断片・挿入位置は含まない（実装詳細は Codex が設計書を読んで判断）

実装完了後、主要ドキュメント反映済みのCHG案件は `docs/archive/` に移動する。

## ドキュメント

- @docs/SPEC.md — 技術仕様書・ドキュメントナビゲーション（タスク別参照ガイドあり）
