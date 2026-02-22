# 実装・レビュー・テスト運用ポリシー

実装の逐次手順（T-1→T-N、Final Gate、Review Packet、自己修正ルール）の正本は `.claude/skills/implementing/SKILL.md` とする。
この文書はレビュー観点とテスト報告ルールの正本として扱う。

## 機能開発フェーズと役割

| フェーズ | スキル | 担当 | 完了の証左 |
|---------|--------|------|-----------|
| 実装 | `/implementing` | Codex | review-note の `## Review Packet` |
| 監査 | `/verify` | Claude Code | review-note の `## Verify` + `判定: PASS` |
| 整地 | `/archiving` | Haiku | アーカイブ完了・コミットハッシュ |
| 承認 | PR レビュー | 人 | MR マージ |

## 禁止事項

`CLAUDE.md` のグローバル禁止事項を参照。

## レビュー観点（二層構造）

### verify 層（設計契約・境界・要件）

`/verify` スキルが担当する。以下を監査し PASS/FAIL を判定する:

- 設計書で定義された API 契約に互換性破壊がないか
- レイヤ依存方向・モジュール境界違反がないか（ArchUnit テスト含む）
- トランザクション境界/非同期方針と不一致がないか
- 要件の必須項目が充足されているか・非スコープ侵入がないか
- review-note の [CONTRACT]/[ARCH] 判断が設計と矛盾していないか

逸脱記法: `[F1] ファイル:行: 内容` / `[F2] ...` （FAILコード付きで記録）

**FAIL時は archive・MR承認に進行不可。** Codex修正→再 verify のサイクルを回す。

### PR レビュー層（実装差分）

人がPRレビューで担当する:

- 触る範囲が task.md に収まっているか
- Done条件を満たしているか（コンパイル通過・テスト通過・動作確認）
- タスク外の変更がないか・未実装タスクがないか

逸脱記法: `[逸脱] T-X: 内容` / `[欠落] T-X` / `[スコープ外] パス: 内容`

## テスト実施

- `回帰テスト`: backend + bff + frontend の破壊的変更検知テスト
- `taskテスト`: task.md に書かれたテスト（Final Gate を含む）

### 回帰テスト

- 機能追加時は、対象機能の `回帰テスト` を追加する
- テストがNGの場合は、機能改修による失敗か誤修正かを判断し、必要なテスト追加・修正を自律的に行う
- テストを追加・修正した場合は、内容を `docs/04_review-note/CHG-XXX.md` に記録する

### taskテスト

- 未実施項目は理由・代替案・残項目を提示し、ユーザー合意を得る
- 完了報告に `[PASS]` / `[FAIL] - 理由` 形式で結果を記載する

詳細:
- `docs/agent-rules/testing-operations.md`
- `docs/agent-rules/playwright-runbook.md`
