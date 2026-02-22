---
name: archiving
description: 実装完了したCHG案件のドキュメントをarchiveに移動し、主要仕様書を更新する。verify PASS済みが前提。
argument-hint: "[CHG番号 例: CHG-011]"
model: haiku
---

# 完了案件のアーカイブ

CHG番号: $ARGUMENTS

## 手順

### 0. 前提確認（ゲート）

**このスキルは `feat/$ARGUMENTS` ブランチが main にマージ済みの状態で、main ブランチ上で実行する。**
実装・verify は feat ブランチで完了しており、ここではドキュメント整地のみを行う。

現在のブランチが main であることを確認する:

```bash
git branch --show-current
```

main でない場合はユーザーに報告して終了する。PR マージ後に再実行するよう案内する。

`docs/04_review-note/$ARGUMENTS.md` の `## Verify` セクションを確認し、`### 判定` 行に `PASS` が含まれることを検証する。

```bash
grep -A1 "### 判定" docs/04_review-note/$ARGUMENTS.md
```

`PASS` が確認できない場合（FAILまたはセクション未存在）は、ユーザーに報告して**ここで終了する**。
先に `/verify $ARGUMENTS` を実行するよう案内する。

### 1. 対象ファイルの特定

`docs/01_requirements/`, `docs/02_designs/`, `docs/03_tasks/` , `docs/04_review-note/` から $ARGUMENTS に一致するファイルを Glob で検索する。
見つからない場合はユーザーに報告して終了する。

要件定義書を読み、変更の概要と影響範囲を把握する。

### 2. ドキュメント更新の判定

要件定義書の内容と verify セクションの「docs更新要否判定」から、更新が必要なドキュメントを [checklist.md](checklist.md) に基づいて判定する。
影響がないドキュメントは読み込まずスキップする。

### 3. 主要ドキュメントの更新

判定結果に基づき、該当するドキュメントのみを更新する。
各ドキュメントの更新対象と記述ルールは [checklist.md](checklist.md) を参照。

IMPORTANT: ドキュメント更新時は checklist.md の「ドキュメント記述ルール」を厳守する。これらのファイルは別セッションで毎回コンテキストに読み込まれるため、冗長な記述はトークンの浪費に直結する。既存の冗長な記述を見つけた場合は、このタイミングで簡潔に書き直す。

IMPORTANT: `docs/SPEC.md` は**変更履歴ログではなく現在状態の記述**である。CHGエントリを末尾に追記してはならない。checklist.md の指示に従い、該当セクション（インフラ構成詳細 / バックエンドアーキテクチャ / 観測性）をマージ更新すること。

### 4. ファイルの移動

```bash
# 要件定義書
mv docs/01_requirements/CHG-XXX_*.md docs/archive/01_requirements/

# 技術設計書
mv docs/02_designs/CHG-XXX_*.md docs/archive/02_designs/

# 実装タスク
mv docs/03_tasks/CHG-XXX_*.md docs/archive/03_tasks/

# review-note
mv docs/04_review-note/CHG-XXX*.md docs/archive/04_review-note/
```

### 5. git commit + push

実装ファイルは PR マージ済みのため、ここではドキュメント変更のみをコミットする:

```bash
# ドキュメント更新分のみ
git add docs/archive/ docs/SPEC.md docs/requirements.md docs/data-model.md \
        docs/ui/ docs/specs/ docs/design-system.md

git status
```

`git status` で **ドキュメント以外の差分がステージされていないこと** を確認する。

変更ファイルを確認し、コミットメッセージを作成する:
- 形式: `{CHG番号} アーカイブ: {案件名の要約}`
- 例: `CHG-015 アーカイブ: OpenTelemetry導入`

```bash
git commit -m "$(cat <<'EOF'
{コミットメッセージ}

Co-Authored-By: Claude Haiku 4.5 <noreply@anthropic.com>
EOF
)"

git push origin "$(git branch --show-current)"
```

### 6. ユーザーへの報告

以下を報告する:
- 移動したファイル一覧
- 更新したドキュメントと変更内容の要約
- 更新をスキップしたドキュメントとその理由
- コミットハッシュ
- push 結果
