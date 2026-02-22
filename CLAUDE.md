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
# Docker（メインの開発フロー）
docker compose up -d          # 全コンテナ起動
docker compose down            # 停止
docker compose logs -f backend # 特定サービスのログ確認

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

新機能は `CHG-XXX_<機能名>.md` の命名規則で以下3ステップのドキュメントを作成する:

1. **要件定義** (`docs/01_requirements/`) — Why・What のみ。技術的 How は書かない
2. **技術設計** (`docs/02_designs/`) — API設計、実装方針、処理フロー
3. **実装タスク** (`docs/03_tasks/`) — 作業単位への分割・触る範囲（クラス名レベル）・Done条件・検証コマンドを明記。コード断片・挿入位置は含まない（実装詳細は Codex が設計書を読んで判断）

ドキュメント作成後は以下の実行フェーズで進める:

```
/implementing CHG-XXX  → Codex が実装（review-note に Review Packet を記録）
/verify CHG-XXX        → Claude Code が監査（PASS/FAIL 判定）
/archiving CHG-XXX     → Haiku がドキュメント整地・archive 移動（verify PASS 必須）
PR レビュー            → 人が承認
```

### 役割マップ

| フェーズ | スキル | 担当モデル |
|---------|--------|-----------|
| 実装 | `/implementing` | Codex |
| 監査 | `/verify` | Claude Code（Sonnet） |
| 整地 | `/archiving` | Haiku |
| 承認 | PR レビュー | 人 |

**IMPORTANT**: `/verify` で FAIL が出た場合、Codex修正→再 `/verify` のサイクルを繰り返す。PASS なしに `/archiving` は実行しない。

## 実装ルール

機能開発は `docs/03_tasks/CHG-XXX_*.md`（task.md）を起点とし、task.md は UTF-8 で読むこと。
実装手順（T-1→T-N、Final Gate、Review Packet、自己修正ルール）の正本は `.claude/skills/implementing/SKILL.md` とする。
実装してユーザーへ完了報告する前に、変更対象に対応するコンテナを必ず `docker compose build` すること（例: `frontend` 変更なら `frontend-admin`/`frontend-customer`、BFF変更なら `customer-bff`/`backoffice-bff`、`backend` 変更なら `backend`）。
起動中コンテナが旧イメージの可能性があるため、build 後は必要に応じて `docker compose up -d` で再作成すること。

### グローバル禁止事項

- 既存コメント（Javadoc・`//`）を削除しない
- SVGアイコン・テキスト・CSS・HTMLタグを変更しない
- エラーメッセージを改変しない（一字一句そのまま）
- import文の順序を変更しない（新規追加はグループ末尾でOK）

## エージェント運用ドキュメント

- `docs/agent-rules/implementation-policy.md` — 実装/レビュー/テスト運用ポリシー
- `docs/agent-rules/testing-operations.md` — テスト実施手順

## エージェントスキル

| スキル | 説明 |
|--------|------|
| `/implementing CHG-XXX` | Codex が task.md に従い実装する |
| `/verify CHG-XXX` | 設計契約・境界・要件を監査し PASS/FAIL を判定する |
| `/archiving CHG-XXX` | verify PASS 後にドキュメント整地・archive 移動・コミットを行う |
| `/designing CHG-XXX` | 要件定義書から技術設計書を生成する |
| `/task-planning CHG-XXX` | 技術設計書から実装タスクファイルを生成する |

各スキルの手順正本: `.claude/skills/<スキル名>/SKILL.md`
レビュー観点・テスト報告ルールの正本: `docs/agent-rules/implementation-policy.md`

## ドキュメント

- @docs/SPEC.md — 技術仕様書・ドキュメントナビゲーション（タスク別参照ガイドあり）
