# AGENTS.md

## プロジェクト構成

- `backend/` — Spring Boot 3.4.2 / Java 21 / PostgreSQL
- `bff/customer-bff/` — NestJS（顧客向けBFF）
- `bff/backoffice-bff/` — NestJS（管理向けBFF）
- `bff/shared/` — 共有DTO・型定義
- `frontend/` — React 19 / TypeScript / Vite / Tailwind CSS 4

ディレクトリ固有の規約は `backend/AGENTS.md`・`frontend/AGENTS.md` 参照。

## コミットメッセージ

日本語で記述。

## 実装ルール

機能開発は `docs/03_tasks/CHG-XXX_*.md`（以下 task.md）を起点とする。task.md は UTF-8 で読むこと。

### 実装手順

1. task.md を冒頭から末尾まで一読し、タスク一覧・検証コマンドを把握する
2. T-1 → T-N の順に1タスクずつ実装する
   - コード断片あり → そのまま使う（compile/test を通すための最小修正は許可。設計書と矛盾する場合は設計書を優先）
   - コード断片なし（薄いフォーマット） → `docs/02_designs/` の設計書を読んで判断する
   - Done条件にテストコマンドあり → テストクラスが未存在なら新規作成。設計書の「テスト観点」を参照する
   - task.md に書かれていない変更はしない（環境起因の自己修正は除く。詳細は `.agents/skills/implementing/SKILL.md` 参照）
3. 検証: `docs/test/testing-operations.md` に従う

### 禁止事項

- 既存コメント（Javadoc・`//`）を削除しない
- SVGアイコン・テキスト・CSS・HTMLタグを変更しない
- エラーメッセージを改変しない（一字一句そのまま）
- import文の順序を変更しない（新規追加はグループ末尾でOK）

### レビュー

task.md と git diff を突き合わせて確認:
- 触る範囲が task.md に収まっているか
- Done条件を満たしているか（コンパイル通過・テスト通過・動作確認）
- 設計書の構造決定（レイヤ依存・トランザクション境界・API契約）に従っているか
- タスク外の変更がないか・未実装タスクがないか

逸脱: `[逸脱] T-X: 内容` / `[欠落] T-X` / `[スコープ外] パス: 内容`

### テスト

- task.md の「テスト手順」を実施する（手動シナリオは省略不可）
- 未実施項目は理由・代替案・残項目を提示しユーザー合意を得る
- 完了報告に実施テスト一覧（`[PASS]` / `[FAIL] - 理由`）を含める
- 詳細は `docs/test/testing-operations.md`・`docs/test/playwright-runbook.md` 参照

## Codex 実行ルール

`/implementing` スキルは `.agents/skills/implementing/SKILL.md` の手順に従う。
