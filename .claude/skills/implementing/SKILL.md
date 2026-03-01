---
name: implementing
description: タスクファイルの全タスクを直列に実装する。[CONTRACT]/[ARCH] は review-note 記録と追加検証が必要。
argument-hint: "[CHG番号 例: CHG-018]"
---

# 実装の実行

CHG番号: $ARGUMENTS

## 手順

### 0. ブランチ準備

初回実装時は main を起点にブランチを作成する:

```bash
git checkout main && git pull origin main
git checkout -b feat/$ARGUMENTS 2>/dev/null || git checkout feat/$ARGUMENTS
```

すでに `feat/$ARGUMENTS` 上にいる場合（再実行・修正対応）はそのまま継続する。

### 1. 読み込み

`docs/03_tasks/` から $ARGUMENTS のタスクファイルを、`docs/02_designs/` から設計書を読み込む。
あわせて `docs/agent-rules/implementation-policy.md` を必ず参照すること（禁止事項・テスト方針の正本）。
見つからない場合はユーザーに報告して終了する。

### 2. タスクの直列実装

T-1 → T-N の順に1タスクずつ実装する。

- 確認・質問不要。自主的に実装する
- `[CONTRACT]`/`[ARCH]` で複数解があり判断不能な場合のみ停止して報告する
- CLAUDE.md のグローバル禁止事項を遵守する
- task.md 外の変更はしない（環境起因の自己修正は除く。下記参照）

**[SAFE]**: 通常の実装のみ。

**[CONTRACT] / [ARCH]**: 実装後に `docs/04_review-note/CHG-XXX.md` の `## T-N` に review-note を追記する。設計書の繰り返しは不要 — 設計書にない実装判断（バージョン選択・例外方針・テストデータ・非自明な選択）のみ記録する。

### DB変更タスクの追加ルール

- DBスキーマ変更を含むCHGでは、`backend/src/main/resources/db/e2e/seed_reference_data.sql` を確認し、**影響テーブルのみ**を更新する。
- DBスキーマ変更を含むCHGでは、`backend/src/main/resources/db/e2e/assert_reference_data.sql` の件数・FK整合アサートを更新する。
- 注文データは DML で固定投入しない。必要な注文は `scripts/e2e_seed.sh` が Customer BFF (`/api/orders`) 経由で作成する。
- DB変更後の検証では `scripts/e2e_seed.sh` を実行し、seed投入 + 件数/FK整合アサート + 注文API生成が通ることを確認する。

### 3. Final Gate

タスクファイルの「Final Gate」コマンドをすべて実行する。
UI 手動確認が必要な場合は MCP Playwright で必ず実施する。Docker Playwright は再現スクリプト化（CI連携・共有）が必要な場合のみ使う。
機能改修が破壊的変更に当たる場合は、関連する回帰テストの追加・修正を必須とする。機能追加時も対象機能の回帰テストを追加する。
テストを追加・修正した場合は、その内容を `docs/04_review-note/CHG-XXX.md` に記録する。
結果の要約を `docs/04_review-note/CHG-XXX.md` の `## Final Gate 結果` に追記する。
失敗した場合は下記「自己修正ルール」に従う。

### 4. Review Packet

`docs/04_review-note/CHG-XXX.md` に追記する:

```markdown
## CHG-XXX review-note
## T-N
（[CONTRACT]/[ARCH] の実装判断）

## Final Gate 結果
（実行コマンドと結果要約）

## Review Packet
### 変更サマリ（10行以内）
### リスクと未解決
### テスト結果（PASS/FAIL、失敗時は30行以内）
```

### 5. commit + push + Draft PR 作成

実装と Review Packet をコミットし、push して Draft PR を作成する:

```bash
git add -u
git add docs/
git commit -m "$(cat <<'EOF'
$ARGUMENTS: <案件名> 実装

EOF
)"
git push -u origin feat/$ARGUMENTS
```

```bash
gh pr create \
  --title "$ARGUMENTS: <案件名>" \
  --body "$(cat <<'EOF'
## 概要
（Review Packet の変更サマリを記載）

## Review Packet
docs/04_review-note/$ARGUMENTS.md を参照。

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)" \
  --draft
```

PR URL を取得して報告に含める。

### 6. 報告

- 実装タスク一覧
- review-note 記録状況（[CONTRACT]/[ARCH] がある場合）
- Final Gate 結果概要・未解決事項
- Draft PR URL

## 自己修正ルール

**してよい（環境起因）**:

| 原因 | 対応 |
|------|------|
| npm パッケージが 404 / バージョン不存在 | 最新安定バージョンに変更して再検証 |
| Docker イメージタグ不存在 | 有効なタグを調べて修正し再検証 |
| lockfile 不整合 | `npm install` で再同期 |
| 自分の変更によるコンパイル・テスト失敗 | 修正して再検証 |

自己修正した場合は Review Packet の「リスクと未解決」に記録する。

**しない（ユーザーに委ねる）**:

| 原因 | 対応 |
|------|------|
| タスク外の既存ロジックによるテスト失敗 | FAIL + 原因を記録してレポート |
| 複数解で判断不能 | 選択肢と調査結果を提示してレポート |
| task.md スコープ超えの変更が必要 | 手を付けずレポート |
