# 認証・認可仕様書

作成日: 2026-02-12
バージョン: 1.0

## 概要

本仕様書は、AI EC Experimentにおける認証・認可システムの振る舞いを定義する。
トークンベース認証、ロールベースアクセス制御（RBAC）、操作履歴の記録について、Given/When/Then形式で明文化する。

**関連変更**: CHG-006 Task1（会員登録とログイン基盤）

---

## 1. 会員登録

### 1-1. 新規会員登録の成功

**Given**:
- メールアドレスがまだ登録されていない
- 有効なメールアドレス、表示名、パスワードが提供される

**When**: POST /api/auth/register を呼び出す
```json
{
  "email": "user@example.com",
  "displayName": "山田太郎",
  "password": "SecurePass123"
}
```

**Then**:
- 新しい `User` レコードが作成される
  - `role` は `CUSTOMER` がデフォルト値として設定される
  - `passwordHash` は BCrypt（強度10）でハッシュ化されて保存される
- 新しい `AuthToken` レコードが作成される
  - UUID v4 トークンが生成される
  - トークンは SHA-256 でハッシュ化されて `tokenHash` に保存される
  - `expiresAt` は現在時刻 + 7日間に設定される
- HTTPステータス 200 が返される
- レスポンスに以下が含まれる:
  - `user` オブジェクト（id, email, displayName, role, createdAt）
  - `token` 文字列（平文のUUID v4、36文字）
  - `expiresAt` 文字列（ISO 8601形式）

**実装箇所**:
- バックエンド: `backend/src/main/java/com/example/aiec/controller/AuthController.java` - register()
- バックエンド: `backend/src/main/java/com/example/aiec/service/AuthService.java` - register()
- バックエンド: `backend/src/main/java/com/example/aiec/entity/User.java` - role デフォルト値
- バックエンド: `backend/src/main/java/com/example/aiec/entity/AuthToken.java`

---

### 1-2. メールアドレス重複時の登録失敗

**Given**: メールアドレスが既に登録されている

**When**: POST /api/auth/register を呼び出す（既存のメールアドレスを使用）

**Then**:
- HTTPステータス 409 (Conflict) が返される
- エラーレスポンス:
```json
{
  "success": false,
  "error": {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "このメールアドレスは既に登録されています"
  }
}
```
- データベースに変更は加えられない

**実装箇所**:
- バックエンド: `AuthService.java` - メールアドレス重複チェック
- バックエンド: `GlobalExceptionHandler.java` - ConflictException ハンドリング

---

## 2. ログイン

### 2-1. ログイン成功

**Given**:
- 有効な会員アカウントが存在する
- 正しいメールアドレスとパスワードが提供される

**When**: POST /api/auth/login を呼び出す
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Then**:
- パスワードが BCrypt で検証される
- 新しい `AuthToken` レコードが作成される
  - UUID v4 トークンが生成される
  - トークンは SHA-256 でハッシュ化されて保存される
  - `expiresAt` は現在時刻 + 7日間に設定される
- `OperationHistory` レコードが作成される
  - `eventType`: LOGIN_SUCCESS
  - `userId`: ログインした会員のID
  - `userEmail`: ログインした会員のメールアドレス
  - `ipAddress`: リクエスト元IPアドレス
- HTTPステータス 200 が返される
- レスポンスに `user`, `token`, `expiresAt` が含まれる

**実装箇所**:
- バックエンド: `AuthController.java` - login()
- バックエンド: `AuthService.java` - login()
- バックエンド: `OperationHistoryService.java` - logLoginSuccess()

---

### 2-2. ログイン失敗（パスワード誤り）

**Given**:
- 有効な会員アカウントが存在する
- 誤ったパスワードが提供される

**When**: POST /api/auth/login を呼び出す

**Then**:
- HTTPステータス 400 (Bad Request) が返される
- エラーレスポンス:
```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "メールアドレスまたはパスワードが正しくありません"
  }
}
```
- `OperationHistory` レコードが作成される
  - `eventType`: LOGIN_FAILURE
  - `userEmail`: 試行されたメールアドレス
  - `ipAddress`: リクエスト元IPアドレス
- トークンは発行されない

**セキュリティ注意**:
- パスワード誤りとアカウント不存在で同じエラーメッセージを返す（アカウント存在判別を防止）

**実装箇所**:
- バックエンド: `AuthService.java` - login() パスワード検証
- バックエンド: `OperationHistoryService.java` - logLoginFailure()

---

### 2-3. ログイン失敗（アカウント不存在）

**Given**: メールアドレスが登録されていない

**When**: POST /api/auth/login を呼び出す

**Then**:
- HTTPステータス 400 (Bad Request) が返される
- エラーメッセージは 2-2 と同一（セキュリティのため）
- `OperationHistory` レコードが作成される
  - `eventType`: LOGIN_FAILURE
  - `userId`: null
  - `userEmail`: 試行されたメールアドレス

**実装箇所**:
- バックエンド: `AuthService.java` - login() ユーザー検索

---

## 3. ログアウト

### 3-1. ログアウト成功

**Given**:
- 有効なトークンが提供される
- トークンが失効していない

**When**: POST /api/auth/logout を呼び出す
```
Authorization: Bearer <token>
```

**Then**:
- トークンのハッシュ値でAuthTokenレコードを検索
- `isRevoked` が `true` に設定される
- `revokedAt` が現在時刻に設定される（ソフトデリート）
- HTTPステータス 200 が返される
- レスポンス:
```json
{
  "success": true,
  "data": {
    "message": "ログアウトしました"
  }
}
```

**実装箇所**:
- バックエンド: `AuthController.java` - logout()
- バックエンド: `AuthService.java` - logout()

---

### 3-2. ログアウト失敗（無効なトークン）

**Given**: 無効または失効済みのトークンが提供される

**When**: POST /api/auth/logout を呼び出す

**Then**:
- HTTPステータス 400 (Bad Request) が返される
- エラーレスポンス:
```json
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "認証が必要です"
  }
}
```

**実装箇所**:
- バックエンド: `AuthService.java` - validateToken()

---

## 4. トークン検証

### 4-1. 有効なトークンの検証成功

**Given**:
- 有効なトークンが提供される
- トークンが有効期限内
- トークンが失効していない（`isRevoked = false`）

**When**: 認証が必要なエンドポイントにリクエストを送信
```
Authorization: Bearer <token>
```

**Then**:
- トークンが SHA-256 でハッシュ化される
- ハッシュ値で `AuthToken` レコードを検索
- トークンが見つかり、有効であることを確認
- 関連する `User` オブジェクトが取得される
- リクエスト処理が続行される

**実装箇所**:
- バックエンド: `AuthService.java` - validateToken()
- バックエンド: `AuthService.java` - findUserByToken()

---

### 4-2. トークン有効期限切れ

**Given**:
- トークンが提供される
- `expiresAt` が現在時刻より過去

**When**: 認証が必要なエンドポイントにリクエストを送信

**Then**:
- HTTPステータス 400 (Bad Request) が返される
- エラーコード: UNAUTHORIZED
- リクエスト処理は中断される

**実装箇所**:
- バックエンド: `AuthService.java` - validateToken() 有効期限チェック

---

### 4-3. 失効済みトークン

**Given**:
- トークンが提供される
- `isRevoked = true`（ログアウト済み）

**When**: 認証が必要なエンドポイントにリクエストを送信

**Then**:
- HTTPステータス 400 (Bad Request) が返される
- エラーコード: UNAUTHORIZED
- リクエスト処理は中断される

**実装箇所**:
- バックエンド: `AuthService.java` - validateToken() 失効チェック

---

## 5. ロールベースアクセス制御（RBAC）

### 5-1. CUSTOMER ロールの権限

**Given**:
- ログイン中の会員の `role` が `CUSTOMER`
- 自分の注文履歴にアクセスする

**When**: GET /api/order/history を呼び出す

**Then**:
- リクエストが成功する（HTTPステータス 200）
- 自分の注文のみが返される（`userId` でフィルタ）

**実装箇所**:
- バックエンド: `OrderController.java` - getOrderHistory()

---

### 5-2. ADMIN ロールの権限（商品管理）

**Given**:
- ログイン中の会員の `role` が `ADMIN`
- 商品情報を編集する

**When**: PUT /api/item/:id を呼び出す

**Then**:
- ロールチェックが成功する
- 商品情報が更新される
- `OperationHistory` レコードが作成される
  - `eventType`: ADMIN_ACTION
  - `details`: "商品編集: [商品名]"
  - `userId`: 管理者のID

**実装箇所**:
- バックエンド: `ItemController.java` - updateItem()
- バックエンド: `AuthService.java` - requireAdmin()

---

### 5-3. 認可エラー（権限不足）

**Given**:
- ログイン中の会員の `role` が `CUSTOMER`
- 管理者のみが実行できる操作を試行する

**When**: PUT /api/item/:id を呼び出す

**Then**:
- HTTPステータス 403 (Forbidden) が返される
- エラーレスポンス:
```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "この操作を実行する権限がありません"
  }
}
```
- `OperationHistory` レコードが作成される
  - `eventType`: AUTHORIZATION_ERROR
  - `details`: リクエストパス
  - `userId`: 試行した会員のID
- 商品情報は変更されない

**実装箇所**:
- バックエンド: `AuthService.java` - requireAdmin()
- バックエンド: `OperationHistoryService.java` - logAuthorizationError()
- バックエンド: `GlobalExceptionHandler.java` - ForbiddenException ハンドリング

---

## 6. 操作履歴（OperationHistory）

### 6-1. LOGIN_SUCCESS イベントの記録

**Given**: ログインが成功した

**When**: ログイン処理が完了する

**Then**:
- `OperationHistory` レコードが作成される
  - `eventType`: LOGIN_SUCCESS
  - `userId`: ログインした会員のID
  - `userEmail`: ログインした会員のメールアドレス
  - `ipAddress`: リクエスト元IPアドレス
  - `requestPath`: "/api/auth/login"
  - `createdAt`: 現在時刻
- トランザクション: `REQUIRES_NEW`（必ず記録される）

**実装箇所**:
- バックエンド: `OperationHistoryService.java` - logLoginSuccess()

---

### 6-2. LOGIN_FAILURE イベントの記録

**Given**: ログインが失敗した（パスワード誤りまたはアカウント不存在）

**When**: ログイン処理が失敗する

**Then**:
- `OperationHistory` レコードが作成される
  - `eventType`: LOGIN_FAILURE
  - `userId`: null または該当会員のID
  - `userEmail`: 試行されたメールアドレス
  - `ipAddress`: リクエスト元IPアドレス
  - `details`: エラー詳細（内部用）
- トランザクション: `REQUIRES_NEW`

**実装箇所**:
- バックエンド: `OperationHistoryService.java` - logLoginFailure()

---

### 6-3. AUTHORIZATION_ERROR イベントの記録

**Given**: 権限不足で操作が拒否された

**When**: 管理者専用エンドポイントにCUSTOMERがアクセスする

**Then**:
- `OperationHistory` レコードが作成される
  - `eventType`: AUTHORIZATION_ERROR
  - `userId`: 試行した会員のID
  - `userEmail`: 試行した会員のメールアドレス
  - `requestPath`: アクセスしようとしたパス
  - `details`: "Role: CUSTOMER, Required: ADMIN"
- トランザクション: `REQUIRES_NEW`
- その後、ForbiddenException がスローされる

**実装箇所**:
- バックエンド: `OperationHistoryService.java` - logAuthorizationError()

---

### 6-4. ADMIN_ACTION イベントの記録

**Given**: 管理者が重要な操作を実行した

**When**: 商品編集、注文ステータス変更などの管理者操作が実行される

**Then**:
- `OperationHistory` レコードが作成される
  - `eventType`: ADMIN_ACTION
  - `userId`: 管理者のID
  - `userEmail`: 管理者のメールアドレス
  - `requestPath`: リクエストパス
  - `details`: 操作の詳細（例: "商品編集: [商品名]"）
- トランザクション: `REQUIRES_NEW`

**実装箇所**:
- バックエンド: `OperationHistoryService.java` - logAdminAction()

---

## 7. パスワードセキュリティ

### 7-1. パスワードハッシュ化

**Given**: 会員登録またはパスワード変更時にパスワードが提供される

**When**: パスワードをデータベースに保存する

**Then**:
- パスワードは BCrypt アルゴリズムでハッシュ化される
- 強度（cost factor）: 10
- ハッシュ化されたパスワードのみが `password_hash` カラムに保存される
- 平文パスワードはメモリに残らない

**実装箇所**:
- バックエンド: `AuthService.java` - BCryptPasswordEncoder 使用

---

### 7-2. パスワード検証

**Given**: ログイン時に平文パスワードが提供される

**When**: 保存されたハッシュと照合する

**Then**:
- BCrypt の `matches()` メソッドで検証される
- 平文パスワードは保存されたハッシュと比較される
- マッチする場合: 認証成功
- マッチしない場合: 認証失敗

**実装箇所**:
- バックエンド: `AuthService.java` - passwordEncoder.matches()

---

## 8. トークンライフサイクル

### 8-1. トークン生成

**Given**: 会員登録またはログインが成功した

**When**: トークンを生成する

**Then**:
- UUID v4 形式のトークンが生成される（36文字）
- トークンは SHA-256 でハッシュ化される（64文字の16進数文字列）
- `AuthToken` レコードが作成される:
  - `userId`: 会員ID
  - `tokenHash`: SHA-256ハッシュ
  - `expiresAt`: 現在時刻 + 7日間
  - `isRevoked`: false
  - `createdAt`: 現在時刻
- 平文トークン（UUID）がクライアントに返される
- ハッシュのみがデータベースに保存される

**実装箇所**:
- バックエンド: `AuthService.java` - generateToken()

---

### 8-2. トークン有効期限

**Given**: トークンが発行されてから7日が経過した

**When**: トークンを使用して認証が必要なエンドポイントにアクセスする

**Then**:
- `expiresAt` と現在時刻が比較される
- 有効期限切れと判定される
- HTTPステータス 400 (Bad Request) が返される
- エラーコード: UNAUTHORIZED

**実装箇所**:
- バックエンド: `AuthService.java` - validateToken()

---

### 8-3. トークン失効（ログアウト）

**Given**: 会員がログアウトした

**When**: ログアウト処理が実行される

**Then**:
- 該当する `AuthToken` レコードが更新される:
  - `isRevoked`: true
  - `revokedAt`: 現在時刻
- 物理削除は行わない（ソフトデリート）
- 失効後、そのトークンは使用できなくなる

**実装箇所**:
- バックエンド: `AuthService.java` - logout()

---

## 9. 会員とゲストの共存

### 9-1. ゲストユーザーのカート

**Given**: ユーザーがログインしていない

**When**: カートに商品を追加する

**Then**:
- `Cart` レコードが作成される:
  - `userId`: null
  - `sessionId`: クライアント生成のセッションID
- `X-Session-Id` ヘッダーでカートを識別する

**実装箇所**:
- バックエンド: `CartService.java` - セッションIDベースのカート管理

---

### 9-2. 会員ユーザーのカート

**Given**: ユーザーがログインしている

**When**: カートに商品を追加する

**Then**:
- `Cart` レコードが作成される:
  - `userId`: ログイン中の会員ID
  - `sessionId`: null または既存のセッションID
- `userId` でカートを識別する

**実装箇所**:
- バックエンド: `CartService.java` - ユーザーIDベースのカート管理

---

### 9-3. カート引き継ぎ（Task3で実装予定）

**Given**:
- ゲストがカートに商品を追加済み（`userId = null`, `sessionId` あり）
- そのゲストがログインまたは会員登録を行う

**When**: ログイン成功時

**Then**: ※Task3で実装予定
- ゲストカート（`sessionId`）と会員カート（`userId`）をマージする
- 重複商品の数量を合算する
- ゲストカートのレコードを削除または会員に紐付ける

**実装箇所**:
- バックエンド: CHG-006 Task3 で実装予定

---

## 10. セキュリティ考慮事項

### 10-1. トークンの保護

- **クライアント側**: トークンは `localStorage` または `sessionStorage` に保存
- **通信**: HTTPS 必須（本番環境）
- **ヘッダー**: `Authorization: Bearer <token>` 形式で送信

### 10-2. パスワードポリシー

- **最小長**: 8文字以上（推奨）
- **ハッシュ化**: BCrypt、強度10
- **平文保存禁止**: パスワードは必ずハッシュ化して保存

### 10-3. 操作履歴の目的

- セキュリティ監査
- 不正アクセスの検知
- コンプライアンス対応
- トランザクション分離（`REQUIRES_NEW`）により確実に記録

### 10-4. ロール管理

- **デフォルトロール**: 新規登録時は `CUSTOMER`
- **ADMIN 昇格**: 手動で行う（自動昇格機能なし）
- **権限チェック**: 管理者専用エンドポイントで必ずロールを検証

---

## 関連ドキュメント

- **技術仕様**: [../SPEC.md](../SPEC.md) - 認証・認可セクション
- **データモデル**: [../data-model.md](../data-model.md) - User, AuthToken, OperationHistory エンティティ
- **API仕様**: [../ui/api-spec.md](../ui/api-spec.md) - 認証APIエンドポイント
- **要件定義**: [../01_requirements/CHG-006_Task1_会員登録とログイン基盤.md](../01_requirements/CHG-006_Task1_会員登録とログイン基盤.md)
- **設計**: [../02_designs/CHG-006_Task1_会員登録とログイン基盤.md](../02_designs/CHG-006_Task1_会員登録とログイン基盤.md)
