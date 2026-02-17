---
name: archiving
description: 実装完了したCHG案件のドキュメントをarchiveに移動し、主要仕様書を更新する。
argument-hint: "[CHG番号 例: CHG-011]"
---

# 完了案件のアーカイブ

CHG番号: $ARGUMENTS

## 手順

### 1. 対象ファイルの特定

`docs/01_requirements/`, `docs/02_designs/`, `docs/03_tasks/` から $ARGUMENTS に一致するファイルを Glob で検索する。
見つからない場合はユーザーに報告して終了する。

要件定義書を読み、変更の概要と影響範囲を把握する。

### 2. ドキュメント更新の判定

要件定義書の内容から、更新が必要なドキュメントを [checklist.md](checklist.md) に基づいて判定する。
影響がないドキュメントは読み込まずスキップする。

### 3. 主要ドキュメントの更新

判定結果に基づき、該当するドキュメントのみを更新する。
各ドキュメントの更新対象と記述ルールは [checklist.md](checklist.md) を参照。

IMPORTANT: ドキュメント更新時は checklist.md の「ドキュメント記述ルール」を厳守する。これらのファイルは別セッションで毎回コンテキストに読み込まれるため、冗長な記述はトークンの浪費に直結する。既存の冗長な記述を見つけた場合は、このタイミングで簡潔に書き直す。

### 4. ファイルの移動

```bash
# 要件定義書
mv docs/01_requirements/CHG-XXX_*.md docs/archive/01_requirements/

# 技術設計書
mv docs/02_designs/CHG-XXX_*.md docs/archive/02_designs/

# 実装タスク
mv docs/03_tasks/CHG-XXX_*.md docs/archive/03_tasks/
```

### 5. ユーザーへの報告

以下を報告する:
- 移動したファイル一覧
- 更新したドキュメントと変更内容の要約
- 更新をスキップしたドキュメントとその理由
