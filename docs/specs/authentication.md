# 認証・認可仕様書

## 概要

AI EC Experimentにおける認証・認可システムの振る舞いを定義する。
トークンベース認証、ロールベースアクセス制御（RBAC）、操作履歴の記録について明文化する。

**関連ドキュメント**: [Customer BFF OpenAPI仕様](../api/customer-bff-openapi.json), [BackOffice BFF OpenAPI仕様](../api/backoffice-bff-openapi.json), [Core API OpenAPI仕様](../api/openapi.json), [データモデル](../data-model.md), [BFFアーキテクチャ](./bff-architecture.md)

---

## 1. 会員登録

### 成功時

- `POST /api/auth/register` — body: `{ email, displayName, password }`
- `User` レコード作成（`role = CUSTOMER`、パスワードは BCrypt 強度10でハッシュ化）
- `AuthToken` レコード作成（UUID v4 → SHA-256 ハッシュで保存、有効期限7日間）
- レスポンス: `{ user, token, expiresAt }` — token は平文UUID（36文字）

### エラー

| 条件 | HTTP | エラーコード |
|------|------|------------|
| メールアドレス重複 | 409 | EMAIL_ALREADY_EXISTS |
| バリデーションエラー | 400 | INVALID_REQUEST |

---

## 2. ログイン

### 成功時

- `POST /api/auth/login` — body: `{ email, password }`
- BCrypt でパスワード検証 → 新しい `AuthToken` レコード作成
- `OperationHistory` に LOGIN_SUCCESS を記録
- レスポンス: `{ user, token, expiresAt }`

### エラー

| 条件 | HTTP | エラーコード | 備考 |
|------|------|------------|------|
| パスワード誤り | 400 | INVALID_CREDENTIALS | アカウント不存在と同じメッセージ（存在判別防止） |
| アカウント不存在 | 400 | INVALID_CREDENTIALS | 同上 |

失敗時は `OperationHistory` に LOGIN_FAILURE を記録（IPアドレス含む）。

---

## 3. ログアウト

- `POST /api/auth/logout` — `Authorization: Bearer <token>` 必須
- トークンの `isRevoked = true` に更新（ソフトデリート）
- 無効/失効済みトークン → エラー: UNAUTHORIZED (400)

---

## 4. トークン検証

| チェック項目 | 失敗時 |
|-------------|--------|
| トークンが存在しない | UNAUTHORIZED (400) |
| `expiresAt` < 現在時刻 | UNAUTHORIZED (400) |
| `isRevoked = true` | UNAUTHORIZED (400) |

検証成功時: トークン → SHA-256ハッシュ → `AuthToken` 検索 → 関連 `User` 取得 → リクエスト処理続行。

---

## 5. トークンライフサイクル

| イベント | 動作 |
|---------|------|
| 登録/ログイン成功 | UUID v4 生成 → SHA-256 でハッシュ化 → `AuthToken` 保存（有効期限7日間） |
| 7日経過 | 有効期限切れ → UNAUTHORIZED |
| ログアウト | `isRevoked = true` に更新（物理削除しない） |

---

## 6. ロールベースアクセス制御（RBAC）

### 顧客 RBAC

| ロール | 操作 | エラー時 |
|--------|------|---------|
| CUSTOMER | 自分の注文履歴取得、カート操作、注文 | — |
| ADMIN | 商品編集、注文ステータス変更 | — |
| CUSTOMERが管理操作 | — | FORBIDDEN (403) + OperationHistory に AUTHORIZATION_ERROR 記録 |

- **デフォルトロール**: 新規登録時は `CUSTOMER`
- **ADMIN 昇格**: 手動のみ（自動昇格機能なし）

### BoUser 権限レベル（管理者）

| 権限レベル | アクセス可能な操作 |
|-----------|----------------|
| OPERATOR | 在庫一覧・調整履歴参照 |
| ADMIN | 在庫調整、注文管理、会員管理 |
| SUPER_ADMIN | ADMIN の全操作 + BoUser 管理（一覧・作成） |

- **顧客トークン拒否**: 顧客トークンで管理APIにアクセスした場合は `CUSTOMER_TOKEN_NOT_ALLOWED` (403)
- BackOffice BFF の `bo-auth.guard.ts` がトークン種別を判定し拒否
- **管理画面の認証復元**: `bo_token` 保持時に `GET /api/bo-auth/me` でログイン状態を復元
- **復元失敗時の契約**: トークンなしは `BFF_UNAUTHORIZED` (401)、不正/期限切れは `BFF_INVALID_TOKEN` (401)

---

## 7. 操作履歴（OperationHistory）

| イベント | eventType | 記録内容 |
|---------|-----------|---------|
| ログイン成功 | LOGIN_SUCCESS | userId, userEmail, ipAddress, requestPath |
| ログイン失敗 | LOGIN_FAILURE | userId(nullの場合あり), userEmail, ipAddress |
| 権限不足 | AUTHORIZATION_ERROR | userId, userEmail, requestPath, "Role: CUSTOMER, Required: ADMIN" |
| 管理操作 | ADMIN_ACTION | userId, userEmail, requestPath, 操作詳細 |

- 全て `REQUIRES_NEW` トランザクションで確実に記録
- 削除禁止（監査証跡として永続保存）

---

## 8. 会員とゲストの共存

| ユーザー種別 | カート識別 | Cart.userId | Cart.sessionId |
|-------------|-----------|-------------|----------------|
| ゲスト | X-Session-Id ヘッダー | null | セッションID |
| 会員 | userId | ログイン中の会員ID | null or 既存 |

**カート引き継ぎ**: ログイン時にゲストカートを会員カートにマージ（重複商品の数量を合算）。

---

## 9. セキュリティ

| 項目 | 仕様 |
|------|------|
| パスワード保存 | BCrypt（強度10）。平文保存禁止 |
| トークン保存（クライアント） | `localStorage` |
| トークン保存（DB） | SHA-256 ハッシュのみ |
| 通信 | HTTPS 必須（本番環境） |
| 認証ヘッダー | `Authorization: Bearer <token>` |

---

---

## 10. BFF 認証トークンキャッシュ（CHG-014〜）

**目的**: Core API `/api/auth/me` への毎リクエスト呼び出しを削減。

**方式**: Read-Through Cache（Redis Key: `auth:token:{tokenHash}`, TTL 1分）
- **認証チェック**: Redis HIT → キャッシュデータを使用、MISS → Core API 呼び出し → Redis 保存
- **ログアウト**: Core API で `isRevoked = true` 更新後、Redis キャッシュを即時削除
- **障害時フォールバック**: Redis 障害時は Core API から直接取得

**整合性**: PostgreSQL が真実のソース。Redis はキャッシュのみ（書き込みなし）。ログアウト反映遅延は最大1分。

---

## 実装クラス一覧

**バックエンド**: `AuthController.java`, `AuthService.java`, `User.java`, `AuthToken.java`, `OperationHistoryService.java`, `GlobalExceptionHandler.java`

**Customer BFF**: `auth.guard.ts`（Redis キャッシュ付きトークン検証）, `redis/redis.service.ts`
