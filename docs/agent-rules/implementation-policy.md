# 実装・レビュー・テスト運用ポリシー

実装の逐次手順（T-1→T-N、Final Gate、Review Packet、自己修正ルール）の正本は `.claude/skills/implementing/SKILL.md` とする。
この文書はレビュー観点とテスト報告ルールの正本として扱う。

## 実装ルール

- 機能開発は `docs/03_tasks/CHG-XXX_*.md` を起点にする
- task.md を UTF-8 で読み、T-1 → T-N を直列に実装する
- task.md 記載外の変更は行わない（環境起因の自己修正を除く）
- `[CONTRACT]`/`[ARCH]` の実装判断、Final Gate結果、Review Packet は `docs/04_review-note/CHG-XXX.md` に記録する

## 禁止事項

- 既存コメント（Javadoc・`//`）を削除しない
- SVGアイコン・テキスト・CSS・HTMLタグを変更しない
- エラーメッセージを改変しない（一字一句そのまま）
- import文の順序を変更しない（新規追加はグループ末尾でOK）

## レビュー観点

- 触る範囲が task.md に収まっているか
- Done条件を満たしているか（コンパイル通過・テスト通過・動作確認）
- 設計書の構造決定（レイヤ依存・トランザクション境界・API契約）に従っているか
- タスク外の変更がないか・未実装タスクがないか

逸脱記法: `[逸脱] T-X: 内容` / `[欠落] T-X` / `[スコープ外] パス: 内容`

## テスト実施

- task.md のテスト手順を実施する（手動シナリオ省略不可）
- 未実施項目は理由・代替案・残項目を提示し、ユーザー合意を得る
- 完了報告に `[PASS]` / `[FAIL] - 理由` 形式で結果を記載する

詳細:
- `docs/agent-rules/testing-operations.md`
- `docs/agent-rules/playwright-runbook.md`
