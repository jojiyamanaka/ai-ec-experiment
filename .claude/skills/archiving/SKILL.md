---
name: archiving
description: 実装完了したCHG案件のドキュメントをarchiveに移動し、主要仕様書を更新する。
argument-hint: "[CHG番号 例: CHG-011]"
model: haiku
---

# 完了案件のアーカイブ

CHG番号: $ARGUMENTS

## 手順

### 1. 対象ファイルの特定

`docs/01_requirements/`, `docs/02_designs/`, `docs/03_tasks/` , `docs/04_review-note/` から $ARGUMENTS に一致するファイルを Glob で検索する。
見つからない場合はユーザーに報告して終了する。

要件定義書を読み、変更の概要と影響範囲を把握する。

### 2. ドキュメント更新の判定

要件定義書の内容から、更新が必要なドキュメントを [checklist.md](checklist.md) に基づいて判定する。
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

以下の手順でコミット・push する:

```bash
# ドキュメント更新分
git add docs/archive/ docs/SPEC.md docs/requirements.md docs/data-model.md \
        docs/ui/ docs/specs/ docs/design-system.md

# 実装ファイル（CHG-XXX の変更分のみ）
git add <backend|bff|frontend の対象ファイル...>

git status
```

`git status` で **CHG-XXX に関係ない差分がステージされていないこと** を確認する。

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
