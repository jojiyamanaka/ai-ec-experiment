# CLAUDE.md

## プロジェクト概要

AIを活用したECサイト実験プロジェクト。React + TypeScript フロントエンド、NestJS BFF、Spring Boot Core API を Docker で動作させる。

## プロジェクト構成

- `backend/` — Spring Boot 3.4.2 / Java 21 / PostgreSQL
- `bff/customer-bff/` — NestJS（顧客向けBFF）
- `bff/backoffice-bff/` — NestJS（管理向けBFF）
- `bff/shared/` — 共有DTO・型定義
- `frontend/` — React 19 / TypeScript / Vite / Tailwind CSS 4

ディレクトリ固有の規約は `backend/AGENTS.md`・`frontend/AGENTS.md` を参照。

## 開発コマンド

```bash
docker compose up -d   # 全コンテナ起動

# フロントエンド（cd frontend）
npm run dev:customer  # 顧客画面（:5173）
npm run dev:admin     # 管理画面（:5174）
```

### アクセスURL
- 顧客画面: http://localhost:5173 → Customer BFF: http://localhost:3001
- 管理画面: http://localhost:5174 → BackOffice BFF: http://localhost:3002
- Core API: `backend:8080`（内部ネットワークのみ）

## コーディング規約

- **コミットメッセージ**: 日本語で記述
- **フロントエンドAPI呼び出し**: IMPORTANT: `fetch` を直接使わないこと。HTTPプリミティブは `@shared/api/client`（`get`/`post`/`put`/`fetchApi`）、エンティティ固有APIは各 `@entities/*/model/api.ts` を使用
- **フロントエンド型定義**: 共通型は `@shared/types/api`、エンティティ型は各 `@entities/*/model/types.ts` に定義（FSD構成）
- **バックエンドDTO**: エンティティと分離し、`fromEntity()` 静的メソッドで変換
- **バックエンド例外**: `ResourceNotFoundException` / `BusinessException` / `ConflictException` をスロー

## 機能開発プロセス

```
/implementing CHG-XXX  → Codex が feat/CHG-XXX ブランチで実装・コミット・Draft PR 作成
/verify CHG-XXX        → Claude Code が監査
PR レビュー            → 人が承認・main へマージ
/archiving CHG-XXX     → Haiku がドキュメント整地・archive 移動（main 上で実行。verify PASS 必須）
```

**IMPORTANT**: PASS なしに `/archiving` は実行しない。`/archiving` は main ブランチ上（PR マージ後）で実行する。

## 実装ルール

実装手順の正本は `.claude/skills/implementing/SKILL.md` とする。
実装してユーザーへ完了報告する前に、変更対象に対応するコンテナを必ず `docker compose build` すること（例: `frontend` 変更なら `frontend-admin`/`frontend-customer`、BFF変更なら `customer-bff`/`backoffice-bff`、`backend` 変更なら `backend`）。
build 後は必要に応じて `docker compose up -d` で再作成すること。

### グローバル禁止事項

- 既存コメント（Javadoc・`//`）を削除しない
- SVGアイコン・テキスト・CSS・HTMLタグを変更しない
- エラーメッセージを改変しない（一字一句そのまま）
- import文の順序を変更しない（新規追加はグループ末尾でOK）

## エージェント運用ドキュメント

- `.claude/skills/<スキル名>/SKILL.md` — 各スキルの手順正本
- `docs/agent-rules/implementation-policy.md` — 実装/レビュー/テスト運用ポリシー
- `docs/agent-rules/testing-operations.md` — テスト実施手順

## ドキュメント

- @docs/SPEC.md — 技術仕様書・ドキュメントナビゲーション（タスク別参照ガイドあり）

Always review in Japanese.