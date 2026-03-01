---
name: verify
description: review-noteを主インプットとして設計書照合・リスク評価・ArchUnit実行を行い、PASS/FAILを判定する。archive開始の前提条件。
argument-hint: "[CHG番号 例: CHG-024]"
---

# 実装監査（Verify）

CHG番号: $ARGUMENTS

## 前提

`docs/04_review-note/$ARGUMENTS.md` が存在すること（implement 完了の証左）。
存在しない場合はユーザーに報告して終了する。

現在のブランチが `feat/$ARGUMENTS` であることを確認する:

```bash
git branch --show-current
```

`feat/$ARGUMENTS` でない場合は `git checkout feat/$ARGUMENTS` でブランチを切り替える。

## 手順

### 1. review-note 読み込み

`docs/04_review-note/$ARGUMENTS.md` を読む。

### 2. 設計書読み込み（T-N 照合用）

`docs/02_designs/$ARGUMENTS_*.md` を読む。

### 3. T-N ノート × 設計書 照合

各タスクノート（T-1, T-2 ...）の実装判断を設計書の構造決定と照合する。

- 設計と異なる選択をした場合、その理由が妥当か評価する
- 設計に明示された契約（API・インターフェース・モジュール境界）と矛盾する場合は FAIL

### 4. リスクと未解決の評価

review-note の「リスクと未解決」セクションを読み、各項目について対応要否を判断してユーザーに報告する。

### 5. テスト結果の評価

review-note の「テスト結果」セクションの FAIL 項目を読み、各 FAIL について対応要否を判断してユーザーに報告する。

### 6. ArchUnit テスト実行

```bash
cd backend && ./mvnw test -Dtest="*ArchTest" -q 2>&1 | tail -20
```

### 7. FAIL 判定

以下のいずれかで FAIL:

| # | 条件 |
|---|------|
| F1 | T-N ノートの実装判断が設計契約と矛盾している |
| F2 | テスト結果の FAIL に「対応必要」と判断したものがある |
| F3 | ArchUnit テストが FAIL |

### 8. 結果記録

`docs/04_review-note/$ARGUMENTS.md` に追記:

```markdown
## Verify

### T-N × 設計書 照合
（乖離があれば列挙。なければ「なし」）

### リスクと未解決の評価
（各項目: 対応不要/対応必要+対応案）

### テスト結果の評価
（各 FAIL 項目: 対応不要/対応必要+対応案）

### ArchUnit
（PASS / FAIL + 出力）

### 判定
PASS / FAIL
```

### 9. review-note + 修正ファイルをコミット・push

```bash
git add -u
git add docs/04_review-note/$ARGUMENTS.md
git commit -m "$(cat <<'EOF'
$ARGUMENTS verify: PASS（または FAIL）

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
git push
```

### 10. ユーザーへ報告

- 判定: PASS / FAIL
- PASS 時: Draft PR を **Ready for Review** に変更して人にレビュー依頼する（`gh pr ready feat/$ARGUMENTS`）
- FAIL 時: 確認必須事項と対応案のリスト。修正後に `/implementing $ARGUMENTS` 再実行または手動修正 → `/verify $ARGUMENTS` 再実行を案内する
