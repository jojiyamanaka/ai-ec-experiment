# AGENTS.md

## テスト手順

テスト実施手順・macOS (zsh/bash) 操作ルールは `docs/test/testing-operations.md` を参照する。  
Playwright の利用方法は `docs/test/playwright-runbook.md` を参照する。

## プロジェクト構成

- `backend/` — Spring Boot 3.4.2 / Java 21 / PostgreSQL（Core API）
- `bff/customer-bff/` — NestJS（顧客向けBFF）
- `bff/backoffice-bff/` — NestJS（管理向けBFF）
- `bff/shared/` — 共有DTO・型定義（workspaceパッケージ）
- `frontend/` — React 19 / TypeScript / Vite / Tailwind CSS 4

## バックエンドの規約

### レイヤー構成

```
controller/  → リクエスト受付、レスポンス返却
service/     → ビジネスロジック
repository/  → DBアクセス（Spring Data JPA）
entity/      → DBテーブル対応
dto/         → リクエスト・レスポンス用の型。エンティティとは分離する
exception/   → カスタム例外
```

パッケージ: `com.example.aiec`

### Lombok

全クラスで使用。以下が頻出:
- エンティティ・DTO: `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`
- サービス: `@RequiredArgsConstructor`
- 例外の追加フィールド: `@Getter`

### 例外処理

カスタム例外をスローすると `GlobalExceptionHandler` が HTTP レスポンスに変換する:

| 例外クラス | HTTPステータス | 用途 |
|-----------|---------------|------|
| `BusinessException` | 400 | ビジネスルール違反 |
| `ResourceNotFoundException` | 404 | リソース不在 |
| `ConflictException` | 409 | 競合 |
| `InsufficientStockException` (extends `ConflictException`) | 409 | 在庫不足（details付き） |

サブクラス例外を追加する場合は `GlobalExceptionHandler` に専用ハンドラーを追加し、親クラスのハンドラーより上に配置する。

### APIレスポンス形式

全エンドポイントが `ApiResponse<T>` を返す:

```java
ApiResponse.success(data)                          // 成功
ApiResponse.error(code, message)                   // エラー
ApiResponse.errorWithDetails(code, message, details) // 詳細付きエラー
```

### DTO変換

エンティティ → DTO は `fromEntity()` 静的メソッドで行う。

## フロントエンドの規約

### API呼び出し

`src/lib/api.ts` の関数を使う。`fetch` を直接使わない。

### 型定義

`src/types/api.ts` に集約。レスポンス型は `ApiResponse<T>` で統一。

### 状態管理

- `ProductContext` — 商品データ
- `CartContext` — カート状態（Customer BFF と同期）

### API接続先（BFF構成）

- 顧客画面: `frontend (5173) -> customer-bff (3001) -> backend (8080/internal)`
- 管理画面: `frontend (5174) -> backoffice-bff (3002) -> backend (8080/internal)`
- ブラウザから Core API (`localhost:8080`) への直接アクセスは行わない

### コーディング

- TypeScript のみ（JS 禁止）
- スタイリングは Tailwind CSS のユーティリティクラス

## コミットメッセージ

日本語で記述。

## 実装・レビュー・テストのルール

機能開発は `docs/03_tasks/CHG-XXX_*.md`（以下 task.md）を起点とする。

### 実装の原則

**必読**: task.md は **UTF-8 エンコーディング** で保存されている。日本語を含むため、ファイル読み込み時は UTF-8 で読むこと。文字化けする場合はエンコーディングを確認する。

1. **タスクファイルの読み込み**
   - task.md を **1回だけ**、冒頭から末尾まで完全に読む
   - タスク一覧 (T-1, T-2, ...) を把握する
   - 検証コマンドをメモする

2. **実装の手順**
   - タスクを **記載順** (T-1 → T-2 → ...) に実装する
   - 各タスクの **パス・コード断片・挿入位置** をそのまま使う。独自の改変をしない
   - task.md に記載のない変更（リファクタ、コメント追加、テスト自作等）はしない
   - 1タスクずつ実装し、都度 Read で変更結果を確認する

3. **禁止事項（重要）**
   - **既存コメントの削除禁止**: task.md に「削除」と指示がない限り、Javadoc（`/** ... */`）やインラインコメント（`//`）を削除しない
   - **既存UIの変更禁止**: task.md に指示がない限り、SVGアイコン・テキスト・CSS・HTMLタグを変更しない
   - **エラーメッセージの改変禁止**: task.md に記載されたエラーメッセージを変更しない（一字一句そのまま使う）
   - **import順の変更禁止**: task.md に記載がない限り、既存のimport文の順序を変更しない（ただし新規追加は該当グループの末尾でOK）

4. **検証**
   - 実施方法は `docs/test/testing-operations.md` に従う

5. **レビュー**
- task.md と git diff を突き合わせ、各タスク項目について以下を確認:
  - 変更ファイルがタスク指定のパスと一致するか
  - コード断片と実装が一致するか（import 順・空白は許容）
  - 指定の挿入位置に配置されているか
  - **タスク外の変更が含まれていないか**（コメント削除・UI変更・テキスト改変など）
  - 未実装のタスクがないか
- 逸脱は `[逸脱] T-X: 内容` / `[欠落] T-X` / `[スコープ外] ファイルパス: 内容` で報告

6. **テスト**
   - task.md の「テスト手順」または「テスト内容」を実施する
   - task.md の手動シナリオを含むテストは省略不可（共通検証コマンドのみで完了扱いにしない）
   - 未実施項目がある場合、完了報告前に理由・代替案・残項目を提示し、ユーザー合意を得る
   - 完了報告には「実施テスト一覧」を必ず含め、各項目を `[PASS]` / `[FAIL] - 理由` で記録する
   - 記録方法は `docs/test/testing-operations.md` を参照する

**スコープ外変更の典型例**:
- Javadoc・インラインコメントの削除
- SVGアイコンのテキスト化
- エラーメッセージ・UI文言の変更
- CSS classの追加・削除
- import文の並び替え

**原則**: task.md に書かれていることだけをする。書かれていないことは既存コードを維持する。
