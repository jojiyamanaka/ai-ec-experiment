---
name: verify
description: 実装完了後の設計契約・要件充足・境界制約を監査し、PASS/FAILを判定する。archive開始の前提条件。
argument-hint: "[CHG番号 例: CHG-024]"
---

# 実装監査（Verify）

CHG番号: $ARGUMENTS

## 前提

`docs/04_review-note/$ARGUMENTS.md` の `## Review Packet` が存在すること（implement 完了の証左）。
存在しない場合はユーザーに報告して終了する。

## 手順

### 1. 監査対象の読み込み

以下を読み込む:

1. `docs/01_requirements/$ARGUMENTS_*.md` — Why・What
2. `docs/02_designs/$ARGUMENTS_*.md` — API契約・処理フロー・構造決定
3. `docs/03_tasks/$ARGUMENTS_*.md` — スコープ定義・Done条件
4. `docs/04_review-note/$ARGUMENTS.md` — 実装判断・Final Gate結果

### 2. 実装差分の取得

```bash
# ブランチ差分のコミット一覧を確認
git log --oneline main..HEAD

# 差分を取得
git diff main...HEAD
```

差分が大きい場合（目安: 500行超）: `git diff main...HEAD -- <対象ファイル>` でファイルごとに分割取得する。
mainと同一ブランチの場合: `git diff HEAD~1..HEAD` で直近コミットを使用する。

### 3. 監査チェック

#### 3-1. 設計契約チェック（API・インターフェース）

設計書のAPIエンドポイント・リクエスト/レスポンス型・ステータスコードと実装差分を照合する。

- エンドポイントのパス・HTTPメソッド一致
- レスポンス型の互換性（フィールド追加はOK、削除・型変更はFAIL）
- エラーレスポンスの契約一致

#### 3-2. モジュール境界・依存方向チェック

設計書の「レイヤ依存・モジュール境界」と実装差分を照合する。

- 他モジュールの `domain.*` 直接参照がないか（`application.port.*` 経由のみ許可）
- UseCase 実装クラスがパッケージプライベートか（`public class` は FAIL）
- クロスモジュール JPA 関連がないか

ArchUnit テストが存在する場合は実行して結果を確認する:

```bash
cd backend && ./mvnw test -Dtest="*ArchTest" -q 2>&1 | tail -20
```

#### 3-3. トランザクション・非同期方針チェック

- 設計書で指定されたトランザクション境界が守られているか
- 非同期処理が設計書の方針（Outbox/JobRunrなど）に従っているか
- 同期/非同期の切り替えが設計と一致しているか

#### 3-4. 要件充足チェック

- 要件定義書の必須項目がすべて実装されているか
- 非スコープ項目（設計書に記載のない変更）が混入していないか
- review-note の [CONTRACT]/[ARCH] 判断が設計書の記述と矛盾していないか

#### 3-5. docs更新要否判定

archiving 時に必要な作業を特定するため、以下を判定してリストアップする:

- `docs/SPEC.md`: アーキテクチャ変更があるか
- `docs/requirements.md`: ビジネスルール変更があるか
- `docs/data-model.md`: エンティティ/スキーマ変更があるか
- `docs/api/*.json`: APIエンドポイント追加/変更があるか
- `docs/ui/customer-ui.md` / `docs/ui/admin-ui.md`: 画面変更があるか
- `docs/specs/*.md`: ドメイン仕様変更があるか

### 4. 判定

**FAIL条件**（1つでも該当で FAIL）:

| # | 条件 |
|---|------|
| F1 | 設計書で定義された契約に互換性破壊がある |
| F2 | レイヤ依存方向・モジュール境界違反がある |
| F3 | トランザクション境界/非同期方針と不一致がある |
| F4 | 要件の必須項目が未達、または非スコープ侵入がある |
| F5 | review-note の [CONTRACT]/[ARCH] 判断が設計と矛盾している |
| F6 | ArchUnit テストが FAIL している |

**PASS条件**（すべて満たす）:

- F1〜F6 がゼロ
- docs更新要否判定が完了し、必要なものがリストアップ済み

### 5. verify結果の記録

`docs/04_review-note/$ARGUMENTS.md` に以下を追記する:

```markdown
## Verify

### 設計契約チェック
（逸脱があれば列挙。なければ「なし」）

### モジュール境界・依存方向チェック
（ArchUnit結果 + 差分確認結果。問題なければ「なし」）

### トランザクション・非同期方針チェック
（逸脱があれば列挙。なければ「なし」）

### 要件充足チェック
（未達・非スコープ侵入があれば列挙。なければ「なし」）

### docs更新要否判定
（要更新ドキュメント一覧。なければ「なし」）

### 判定
PASS / FAIL

### @codex 修正依頼
（FAIL時のみ記載）
CHG: $ARGUMENTS
逸脱箇所:
- [ファイル:行] 逸脱内容（F? に対応）
期待する修正:
- 具体的な修正指示
確認コマンド:
- 修正後に実行すべきコマンド
```

### 6. ユーザーへの報告

- 監査結果サマリ（逸脱一覧または「逸脱なし」）
- 判定: **PASS** / **FAIL**
- PASS時: archive 実行可能（`/archiving $ARGUMENTS`）
- FAIL時: Codex修正後に再 verify が必要（`/verify $ARGUMENTS`）
- docs更新要否リスト（archive時の作業参考情報）
