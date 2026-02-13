# CHG-008: ドメイン分離とBoUser管理 - 技術設計

## 概要

本設計では、顧客（Customer）と管理者（BackOffice User）のドメイン分離、および BoUser の別テーブル管理を実現します。

---

## 1. データモデル設計

### 1.1 bo_users テーブル（新設）

管理者専用のユーザーテーブル。

```sql
CREATE TABLE bo_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    permission_level VARCHAR(50) NOT NULL DEFAULT 'OPERATOR', -- SUPER_ADMIN, ADMIN, OPERATOR
    last_login_at DATETIME,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bo_users_email ON bo_users(email);
CREATE INDEX idx_bo_users_is_active ON bo_users(is_active);
```

#### フィールド説明

| フィールド | 型 | NULL | 説明 |
|-----------|-----|------|------|
| id | INTEGER | NOT NULL | プライマリキー（自動採番） |
| email | VARCHAR(255) | NOT NULL | メールアドレス（ログイン ID、ユニーク） |
| password_hash | VARCHAR(255) | NOT NULL | bcrypt でハッシュ化されたパスワード |
| display_name | VARCHAR(100) | NOT NULL | 表示名 |
| permission_level | VARCHAR(50) | NOT NULL | 権限レベル（SUPER_ADMIN, ADMIN, OPERATOR） |
| last_login_at | DATETIME | NULL | 最終ログイン日時 |
| is_active | BOOLEAN | NOT NULL | 有効/無効フラグ |
| created_at | DATETIME | NOT NULL | 作成日時 |
| updated_at | DATETIME | NOT NULL | 更新日時 |

#### 権限レベル（permission_level）

現時点ではシンプルな enum で管理。将来的に RBAC（bo_roles / bo_permissions）に拡張可能。

- **SUPER_ADMIN**: スーパー管理者（全権限）
- **ADMIN**: 管理者（BoUser 管理以外の全権限）
- **OPERATOR**: オペレーター（参照権限のみ、一部編集権限）

### 1.2 bo_auth_tokens テーブル（新設）

BoUser の認証トークン管理テーブル。

```sql
CREATE TABLE bo_auth_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bo_user_id INTEGER NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bo_user_id) REFERENCES bo_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_bo_auth_tokens_token_hash ON bo_auth_tokens(token_hash);
CREATE INDEX idx_bo_auth_tokens_bo_user_id ON bo_auth_tokens(bo_user_id);
CREATE INDEX idx_bo_auth_tokens_expires_at ON bo_auth_tokens(expires_at);
```

#### フィールド説明

| フィールド | 型 | NULL | 説明 |
|-----------|-----|------|------|
| id | INTEGER | NOT NULL | プライマリキー（自動採番） |
| bo_user_id | INTEGER | NOT NULL | BoUser の外部キー |
| token_hash | VARCHAR(255) | NOT NULL | トークンのハッシュ値（SHA-256）|
| expires_at | DATETIME | NOT NULL | 有効期限（例: 7日間） |
| is_revoked | BOOLEAN | NOT NULL | 失効フラグ（ログアウト時に TRUE） |
| created_at | DATETIME | NOT NULL | 作成日時 |

#### トークン設計の詳細

- **生成**: UUID v4 でランダムなトークンを生成（例: `550e8400-e29b-41d4-a716-446655440000`）
- **保存**: 生トークンは保存せず、SHA-256 でハッシュ化した値を `token_hash` に保存
- **検証**: クライアントから送られた生トークンをハッシュ化し、`token_hash` と照合
- **有効期限**: デフォルト 7日間（`expires_at` で管理）
- **失効**: ログアウト時に `is_revoked = TRUE` を設定

### 1.3 users テーブル（変更）

顧客専用テーブルに変更。`role` フィールドを削除。

```sql
-- 既存のテーブルから role カラムを削除
ALTER TABLE users DROP COLUMN role;
```

変更後のスキーマ：

```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### 1.4 ER図

```
┌─────────────────┐          ┌──────────────────────┐
│   bo_users      │          │  bo_auth_tokens      │
├─────────────────┤          ├──────────────────────┤
│ id (PK)         │◄─────────┤ id (PK)              │
│ email           │          │ bo_user_id (FK)      │
│ password_hash   │          │ token_hash (UNIQUE)  │
│ display_name    │          │ expires_at           │
│ permission_level│          │ is_revoked           │
│ last_login_at   │          │ created_at           │
│ is_active       │          └──────────────────────┘
│ created_at      │
│ updated_at      │
└─────────────────┘

┌─────────────────┐          ┌──────────────────────┐
│   users         │          │  auth_tokens         │
├─────────────────┤          ├──────────────────────┤
│ id (PK)         │◄─────────┤ id (PK)              │
│ email           │          │ user_id (FK)         │
│ password_hash   │          │ token_hash (UNIQUE)  │
│ display_name    │          │ expires_at           │
│ is_active       │          │ is_revoked           │
│ created_at      │          │ created_at           │
│ updated_at      │          └──────────────────────┘
└─────────────────┘
```

---

## 2. API 設計

### 2.1 API エンドポイント構成

#### 顧客向け API（既存）

| メソッド | エンドポイント | 認証 | 説明 |
|---------|---------------|------|------|
| POST | `/api/auth/register` | なし | 顧客の新規登録 |
| POST | `/api/auth/login` | なし | 顧客のログイン |
| POST | `/api/auth/logout` | User | 顧客のログアウト |
| GET | `/api/auth/me` | User | 顧客の情報取得 |
| GET | `/api/item` | なし | 商品一覧取得 |
| POST | `/api/order/cart` | なし/User | カート追加 |
| POST | `/api/order/reg` | なし/User | 注文確定 |
| GET | `/api/order/history` | User | 注文履歴取得 |

#### 管理者向け API（新設・移行）

**認証 API**

| メソッド | エンドポイント | 認証 | 説明 |
|---------|---------------|------|------|
| POST | `/api/bo-auth/login` | なし | BoUser ログイン |
| POST | `/api/bo-auth/logout` | BoUser | BoUser ログアウト |
| GET | `/api/bo-auth/me` | BoUser | BoUser 情報取得 |

**管理 API（既存の /api/admin/** を移行）**

| メソッド | エンドポイント | 認証 | 説明 |
|---------|---------------|------|------|
| GET | `/api/bo/admin/members` | BoUser | 顧客一覧取得 |
| GET | `/api/bo/admin/members/{id}` | BoUser | 顧客詳細取得 |
| PUT | `/api/bo/admin/members/{id}/status` | BoUser | 顧客状態変更 |
| GET | `/api/bo/admin/inventory` | BoUser | 在庫一覧取得 |
| POST | `/api/bo/admin/inventory/adjust` | BoUser | 在庫調整 |
| GET | `/api/bo/admin/orders` | BoUser | 注文一覧取得 |
| PUT | `/api/bo/admin/orders/{id}/confirm` | BoUser | 注文確認 |

**BoUser 管理 API（新設）**

| メソッド | エンドポイント | 認証 | 権限 | 説明 |
|---------|---------------|------|------|------|
| GET | `/api/bo/bo-users` | BoUser | SUPER_ADMIN | BoUser 一覧取得 |
| POST | `/api/bo/bo-users` | BoUser | SUPER_ADMIN | BoUser 新規登録 |
| GET | `/api/bo/bo-users/{id}` | BoUser | SUPER_ADMIN | BoUser 詳細取得 |
| PUT | `/api/bo/bo-users/{id}` | BoUser | SUPER_ADMIN | BoUser 更新 |
| PUT | `/api/bo/bo-users/{id}/status` | BoUser | SUPER_ADMIN | BoUser 状態変更 |
| DELETE | `/api/bo/bo-users/{id}` | BoUser | SUPER_ADMIN | BoUser 削除（論理削除） |

### 2.2 BoAuth API 詳細

#### POST /api/bo-auth/login

BoUser のログイン。

**リクエスト**

```json
{
  "email": "admin@example.com",
  "password": "password123"
}
```

**レスポンス（成功）**

```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "email": "admin@example.com",
      "displayName": "管理者太郎",
      "permissionLevel": "ADMIN",
      "isActive": true,
      "createdAt": "2025-01-01T00:00:00",
      "updatedAt": "2025-01-01T00:00:00"
    },
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "expiresAt": "2025-01-08T00:00:00"
  }
}
```

**レスポンス（失敗）**

```json
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "メールアドレスまたはパスワードが正しくありません"
  }
}
```

#### POST /api/bo-auth/logout

BoUser のログアウト（トークン失効）。

**リクエストヘッダー**

```
Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000
```

**レスポンス（成功）**

```json
{
  "success": true,
  "data": {
    "message": "ログアウトしました"
  }
}
```

#### GET /api/bo-auth/me

BoUser の情報取得。

**リクエストヘッダー**

```
Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000
```

**レスポンス（成功）**

```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "admin@example.com",
    "displayName": "管理者太郎",
    "permissionLevel": "ADMIN",
    "lastLoginAt": "2025-01-01T10:00:00",
    "isActive": true,
    "createdAt": "2025-01-01T00:00:00",
    "updatedAt": "2025-01-01T00:00:00"
  }
}
```

### 2.3 エラーコード定義

#### BoAuth 関連

| コード | HTTP ステータス | 説明 |
|-------|----------------|------|
| `INVALID_CREDENTIALS` | 401 | メールアドレスまたはパスワードが正しくない |
| `BO_USER_NOT_FOUND` | 404 | BoUser が見つからない |
| `BO_USER_INACTIVE` | 403 | BoUser が無効化されている |
| `UNAUTHORIZED` | 401 | 認証が必要 |
| `INVALID_TOKEN` | 401 | トークンが無効 |
| `TOKEN_EXPIRED` | 401 | トークンの有効期限切れ |
| `TOKEN_REVOKED` | 401 | トークンが失効済み |

#### 管理 API 関連

| コード | HTTP ステータス | 説明 |
|-------|----------------|------|
| `FORBIDDEN` | 403 | 権限不足 |
| `INSUFFICIENT_PERMISSION` | 403 | 権限レベルが不足している |
| `CUSTOMER_TOKEN_NOT_ALLOWED` | 403 | 顧客トークンでは管理 API にアクセスできない |

---

## 3. 認証・認可設計

### 3.1 認証フィルタの構成

#### BoUserAuthenticationFilter

`/api/bo/**` に対するリクエストをインターセプトし、BoUser 認証を行う。

**処理フロー**

```
1. リクエストヘッダーから Authorization: Bearer <token> を取得
2. トークンがなければ 401 を返す
3. トークンを SHA-256 でハッシュ化
4. bo_auth_tokens テーブルから token_hash で検索
5. トークンが存在しない → 401 (INVALID_TOKEN)
6. is_revoked が TRUE → 401 (TOKEN_REVOKED)
7. expires_at が過去 → 401 (TOKEN_EXPIRED)
8. bo_user_id から BoUser を取得
9. BoUser が is_active = FALSE → 403 (BO_USER_INACTIVE)
10. リクエストコンテキストに BoUser を設定
11. 次の処理に進む
```

**実装場所**

- パッケージ: `com.example.aiec.security`
- クラス: `BoUserAuthenticationFilter` (extends `OncePerRequestFilter`)

#### CustomerAuthenticationFilter（既存）

`/api/**`（`/api/bo/**` を除く）に対するリクエストをインターセプトし、User 認証を行う。

**処理フロー**

既存の認証フィルタと同様。ただし、`/api/bo/**` にはマッチしないように設定。

### 3.2 権限チェックの集約

権限チェックは Controller に散らさず、以下のいずれかで集約：

#### アプローチ 1: アノテーションベース（推奨）

Spring Security の `@PreAuthorize` を使用。

```java
@RestController
@RequestMapping("/api/bo/bo-users")
public class BoUserController {

    @PreAuthorize("hasPermission('SUPER_ADMIN')")
    @GetMapping
    public ApiResponse<List<BoUserDto>> getAllBoUsers() {
        // ...
    }
}
```

#### アプローチ 2: サービス層で権限チェック

```java
@Service
public class BoUserService {

    public List<BoUser> getAllBoUsers(BoUser currentUser) {
        if (currentUser.getPermissionLevel() != PermissionLevel.SUPER_ADMIN) {
            throw new ForbiddenException("INSUFFICIENT_PERMISSION", "この操作にはスーパー管理者権限が必要です");
        }
        // ...
    }
}
```

**推奨**: アプローチ 1（アノテーションベース）を採用し、Spring Security の設定で一元管理。

### 3.3 トークン管理の実装

#### トークン生成（BoAuthService）

```java
public class BoAuthService {

    public TokenPair createToken(BoUser boUser) {
        // 1. UUID v4 でランダムなトークンを生成
        String rawToken = UUID.randomUUID().toString();

        // 2. SHA-256 でハッシュ化
        String tokenHash = hashToken(rawToken);

        // 3. 有効期限を設定（7日間）
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        // 4. bo_auth_tokens テーブルに保存
        BoAuthToken authToken = new BoAuthToken();
        authToken.setBoUserId(boUser.getId());
        authToken.setTokenHash(tokenHash);
        authToken.setExpiresAt(expiresAt);
        authToken.setIsRevoked(false);
        boAuthTokenRepository.save(authToken);

        // 5. 生トークンとハッシュのペアを返す
        return new TokenPair(rawToken, authToken);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
```

#### トークン検証

```java
public BoUser verifyToken(String rawToken) {
    // 1. トークンをハッシュ化
    String tokenHash = hashToken(rawToken);

    // 2. bo_auth_tokens から検索
    BoAuthToken authToken = boAuthTokenRepository.findByTokenHash(tokenHash)
        .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "無効なトークンです"));

    // 3. 失効チェック
    if (authToken.getIsRevoked()) {
        throw new BusinessException("TOKEN_REVOKED", "このトークンは失効しています");
    }

    // 4. 有効期限チェック
    if (authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
        throw new BusinessException("TOKEN_EXPIRED", "トークンの有効期限が切れています");
    }

    // 5. BoUser を取得
    BoUser boUser = boUserRepository.findById(authToken.getBoUserId())
        .orElseThrow(() -> new ResourceNotFoundException("BO_USER_NOT_FOUND", "BoUserが見つかりません"));

    // 6. 有効チェック
    if (!boUser.getIsActive()) {
        throw new ForbiddenException("BO_USER_INACTIVE", "このアカウントは無効化されています");
    }

    return boUser;
}
```

---

## 4. 実装方針

### 4.1 新規作成クラス

#### エンティティ

- `com.example.aiec.entity.BoUser` - BoUser エンティティ
- `com.example.aiec.entity.BoAuthToken` - BoUser 認証トークンエンティティ
- `com.example.aiec.entity.PermissionLevel` (enum) - 権限レベル

#### リポジトリ

- `com.example.aiec.repository.BoUserRepository`
- `com.example.aiec.repository.BoAuthTokenRepository`

#### サービス

- `com.example.aiec.service.BoUserService` - BoUser のビジネスロジック
- `com.example.aiec.service.BoAuthService` - BoUser 認証・トークン管理

#### コントローラー

- `com.example.aiec.controller.BoAuthController` - BoUser 認証 API
- `com.example.aiec.controller.BoUserController` - BoUser 管理 API

#### DTO

- `com.example.aiec.dto.BoUserDto` - BoUser レスポンス用 DTO
- `com.example.aiec.dto.BoAuthResponse` - BoAuth レスポンス用 DTO
- `com.example.aiec.dto.BoLoginRequest` - BoUser ログインリクエスト DTO

#### セキュリティ

- `com.example.aiec.security.BoUserAuthenticationFilter` - BoUser 認証フィルタ

#### 例外

- `com.example.aiec.exception.InsufficientPermissionException` - 権限不足例外

### 4.2 既存クラスの変更

#### エンティティ

- `com.example.aiec.entity.User`
  - `role` フィールドを削除
  - Role enum への依存を削除

#### コントローラー

- `com.example.aiec.controller.AdminController`
  - `/api/admin/**` → `/api/bo/admin/**` に変更
  - User 認証 → BoUser 認証に変更

- `com.example.aiec.controller.AdminInventoryController`
  - `/api/admin/inventory/**` → `/api/bo/admin/inventory/**` に変更
  - User 認証 → BoUser 認証に変更

#### サービス

- `com.example.aiec.service.OperationHistoryService`
  - BoUser の操作履歴を記録できるようにメソッドを追加

### 4.3 削除するクラス

- `com.example.aiec.entity.Role` (enum) - BoUser 側では使用しないため削除

---

## 5. 処理フロー

### 5.1 BoUser ログインフロー

```
┌─────────┐                ┌──────────────┐              ┌─────────────┐              ┌──────────────┐
│ Client  │                │ BoAuth       │              │ BoAuth      │              │ bo_users     │
│         │                │ Controller   │              │ Service     │              │ Table        │
└────┬────┘                └──────┬───────┘              └──────┬──────┘              └──────┬───────┘
     │                            │                             │                            │
     │ POST /api/bo-auth/login    │                             │                            │
     ├───────────────────────────►│                             │                            │
     │ { email, password }        │                             │                            │
     │                            │                             │                            │
     │                            │ login(email, password)      │                            │
     │                            ├────────────────────────────►│                            │
     │                            │                             │                            │
     │                            │                             │ findByEmail(email)         │
     │                            │                             ├───────────────────────────►│
     │                            │                             │                            │
     │                            │                             │◄───────────────────────────┤
     │                            │                             │ BoUser                     │
     │                            │                             │                            │
     │                            │                             │ verifyPassword()           │
     │                            │                             │ (bcrypt check)             │
     │                            │                             │                            │
     │                            │                             │ createToken(boUser)        │
     │                            │                             │ - UUID生成                  │
     │                            │                             │ - SHA-256ハッシュ化         │
     │                            │                             │ - bo_auth_tokensに保存      │
     │                            │                             │                            │
     │                            │◄────────────────────────────┤                            │
     │                            │ { boUser, rawToken }        │                            │
     │                            │                             │                            │
     │◄───────────────────────────┤                             │                            │
     │ { success, data: { user, token, expiresAt } }            │                            │
     │                            │                             │                            │
```

### 5.2 管理 API アクセスフロー

```
┌─────────┐      ┌────────────────┐      ┌──────────────┐      ┌────────────┐      ┌──────────┐
│ Client  │      │ BoUser Auth    │      │ BoAdmin      │      │ BoAuth     │      │ bo_auth_ │
│         │      │ Filter         │      │ Controller   │      │ Service    │      │ tokens   │
└────┬────┘      └────────┬───────┘      └──────┬───────┘      └─────┬──────┘      └─────┬────┘
     │                    │                     │                     │                   │
     │ GET /api/bo/admin/members                │                     │                   │
     │ Authorization: Bearer <token>            │                     │                   │
     ├───────────────────►│                     │                     │                   │
     │                    │                     │                     │                   │
     │                    │ extractToken()      │                     │                   │
     │                    │                     │                     │                   │
     │                    │ verifyToken(token)  │                     │                   │
     │                    ├─────────────────────┼────────────────────►│                   │
     │                    │                     │                     │                   │
     │                    │                     │                     │ hashToken()       │
     │                    │                     │                     │                   │
     │                    │                     │                     │ findByTokenHash() │
     │                    │                     │                     ├──────────────────►│
     │                    │                     │                     │                   │
     │                    │                     │                     │◄──────────────────┤
     │                    │                     │                     │ BoAuthToken       │
     │                    │                     │                     │                   │
     │                    │                     │                     │ check:            │
     │                    │                     │                     │ - is_revoked      │
     │                    │                     │                     │ - expires_at      │
     │                    │                     │                     │ - BoUser.is_active│
     │                    │                     │                     │                   │
     │                    │◄────────────────────┼─────────────────────┤                   │
     │                    │ BoUser              │                     │                   │
     │                    │                     │                     │                   │
     │                    │ setContext(boUser)  │                     │                   │
     │                    │                     │                     │                   │
     │                    │ proceed()           │                     │                   │
     │                    ├────────────────────►│                     │                   │
     │                    │                     │                     │                   │
     │                    │                     │ getMembers()        │                   │
     │                    │                     │                     │                   │
     │◄───────────────────┼─────────────────────┤                     │                   │
     │ { success, data: [users] }               │                     │                   │
     │                    │                     │                     │                   │
```

### 5.3 誤アクセス防止（顧客トークンで管理 API にアクセス）

```
┌─────────┐      ┌────────────────┐
│ Client  │      │ BoUser Auth    │
│         │      │ Filter         │
└────┬────┘      └────────┬───────┘
     │                    │
     │ GET /api/bo/admin/members
     │ Authorization: Bearer <customer_token>
     ├───────────────────►│
     │                    │
     │                    │ extractToken()
     │                    │ verifyToken(customer_token)
     │                    │
     │                    │ hashToken()
     │                    │ → bo_auth_tokens には存在しない
     │                    │
     │◄───────────────────┤
     │ 401 INVALID_TOKEN  │
     │ または 403 CUSTOMER_TOKEN_NOT_ALLOWED
     │                    │
```

**補足**: 顧客トークンと BoUser トークンを完全に分離するため、`bo_auth_tokens` テーブルに存在しないトークンは即座に 401 を返す。

---

## 6. マイグレーション計画

### 6.1 データベースマイグレーション

#### Step 1: bo_users / bo_auth_tokens テーブル作成

```sql
-- bo_users テーブル作成
CREATE TABLE bo_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    permission_level VARCHAR(50) NOT NULL DEFAULT 'OPERATOR',
    last_login_at DATETIME,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bo_users_email ON bo_users(email);
CREATE INDEX idx_bo_users_is_active ON bo_users(is_active);

-- bo_auth_tokens テーブル作成
CREATE TABLE bo_auth_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bo_user_id INTEGER NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bo_user_id) REFERENCES bo_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_bo_auth_tokens_token_hash ON bo_auth_tokens(token_hash);
CREATE INDEX idx_bo_auth_tokens_bo_user_id ON bo_auth_tokens(bo_user_id);
CREATE INDEX idx_bo_auth_tokens_expires_at ON bo_auth_tokens(expires_at);
```

#### Step 2: 既存 User（Role=ADMIN）を BoUser に移行

```sql
-- 既存の管理者を bo_users に移行
INSERT INTO bo_users (email, password_hash, display_name, permission_level, is_active, created_at, updated_at)
SELECT email, password_hash, display_name, 'ADMIN', is_active, created_at, updated_at
FROM users
WHERE role = 'ADMIN';
```

#### Step 3: users テーブルから Role=ADMIN のレコードを削除

```sql
-- 管理者レコードを削除（顧客のみ残す）
DELETE FROM users WHERE role = 'ADMIN';
```

#### Step 4: users テーブルから role カラムを削除

```sql
-- SQLite では ALTER TABLE DROP COLUMN がサポートされていないため、テーブル再作成
-- 1. 新しいテーブルを作成
CREATE TABLE users_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. データをコピー
INSERT INTO users_new (id, email, password_hash, display_name, is_active, created_at, updated_at)
SELECT id, email, password_hash, display_name, is_active, created_at, updated_at
FROM users;

-- 3. 古いテーブルを削除
DROP TABLE users;

-- 4. 新しいテーブルをリネーム
ALTER TABLE users_new RENAME TO users;

-- 5. インデックスを再作成
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);
```

### 6.2 ロールバック手順

万が一問題が発生した場合のロールバック手順。

#### Step 1: bo_users から users に戻す

```sql
-- bo_users のデータを users に戻す（role カラムを再追加）
ALTER TABLE users ADD COLUMN role VARCHAR(50);

INSERT INTO users (email, password_hash, display_name, role, is_active, created_at, updated_at)
SELECT email, password_hash, display_name, 'ADMIN', is_active, created_at, updated_at
FROM bo_users;
```

#### Step 2: bo_users / bo_auth_tokens テーブルを削除

```sql
DROP TABLE bo_auth_tokens;
DROP TABLE bo_users;
```

---

## 7. キャッシュ制御

### 7.1 管理 API のキャッシュ無効化

すべての管理 API レスポンスに以下のヘッダーを設定：

```
Cache-Control: no-store, no-cache, must-revalidate
Pragma: no-cache
Expires: 0
```

**実装方法**: Spring の `ResponseEntity` または `@RestControllerAdvice` で一括設定。

```java
@RestControllerAdvice
public class BoApiResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // /api/bo/** のパスにマッチする場合のみ適用
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   ServerHttpRequest request, ServerHttpResponse response) {
        response.getHeaders().add("Cache-Control", "no-store, no-cache, must-revalidate");
        response.getHeaders().add("Pragma", "no-cache");
        response.getHeaders().add("Expires", "0");
        return body;
    }
}
```

### 7.2 URL パスの分離原則

**同じ URL で admin/非 admin のレスポンスを変えない**

- 顧客向け: `/api/**`
- 管理者向け: `/api/bo/**`

この設計により、URL が異なるため、キャッシュの混在は発生しない。

---

## 8. CORS / セキュリティ設定

### 8.1 CORS 設定

#### 顧客向け API（/api/**）

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:5173", "https://example.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

#### 管理者向け API（/api/bo/**）

```java
registry.addMapping("/api/bo/**")
    .allowedOrigins("http://localhost:5174", "https://admin.example.com")
    .allowedMethods("GET", "POST", "PUT", "DELETE")
    .allowedHeaders("*")
    .allowCredentials(false); // Bearer トークン運用のため Cookie は使用しない
```

### 8.2 Content-Security-Policy（CSP）

XSS 対策として、管理画面に CSP を設定。

```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;
```

---

## 9. テスト計画

### 9.1 誤アクセス防止テスト

#### テストケース 1: 顧客トークンで管理 API にアクセス

```bash
# 顧客トークンを取得
POST /api/auth/login
{ "email": "customer@example.com", "password": "password" }
→ token: "customer-token-12345"

# 管理 API にアクセス
GET /api/bo/admin/members
Authorization: Bearer customer-token-12345

# 期待結果
401 INVALID_TOKEN または 403 CUSTOMER_TOKEN_NOT_ALLOWED
```

#### テストケース 2: BoUser トークンで顧客 API にアクセス

```bash
# BoUser トークンを取得
POST /api/bo-auth/login
{ "email": "admin@example.com", "password": "password" }
→ token: "bo-token-67890"

# 顧客 API にアクセス
GET /api/item
Authorization: Bearer bo-token-67890

# 期待結果
設計方針次第:
- 許可する場合: 200 OK（商品一覧を返す）
- 拒否する場合: 403 FORBIDDEN
```

#### テストケース 3: トークンなしでアクセス

```bash
GET /api/bo/admin/members

# 期待結果
401 UNAUTHORIZED
```

#### テストケース 4: トークン失効後にアクセス

```bash
# ログアウト
POST /api/bo-auth/logout
Authorization: Bearer bo-token-67890

# 再度アクセス
GET /api/bo/admin/members
Authorization: Bearer bo-token-67890

# 期待結果
401 TOKEN_REVOKED
```

### 9.2 キャッシュ制御テスト

```bash
GET /api/bo/admin/members
Authorization: Bearer bo-token-67890

# 期待結果（レスポンスヘッダー）
Cache-Control: no-store, no-cache, must-revalidate
Pragma: no-cache
Expires: 0
```

---

## 10. 既存パターンとの整合性

### 10.1 認証トークンのハッシュ化

既存の `auth_tokens` テーブルでは生トークンを保存していた場合、これを修正し、BoUser と同様にハッシュ化する。

**修正内容**:
- `auth_tokens.token` → `auth_tokens.token_hash` に変更
- トークン生成時に SHA-256 でハッシュ化
- トークン検証時にクライアントから送られた生トークンをハッシュ化して照合

### 10.2 ApiResponse 形式の統一

既存の `ApiResponse<T>` 形式を BoAuth API でも使用。

```java
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorDetail error;
}
```

### 10.3 例外ハンドリングの統一

既存の `GlobalExceptionHandler` を拡張し、BoUser 関連の例外もハンドリング。

```java
@ExceptionHandler(InsufficientPermissionException.class)
public ResponseEntity<ApiResponse<Void>> handleInsufficientPermission(InsufficientPermissionException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
}
```

---

## 11. まとめ

### 主要な設計ポイント

1. **テーブル分離**: `users` は顧客専用、`bo_users` は管理者専用
2. **トークン管理**: 生トークンは保存せず、SHA-256 でハッシュ化
3. **API エンドポイント分離**: `/api/**`（顧客）と `/api/bo/**`（管理者）
4. **認証フィルタ**: `/api/bo/**` には BoUser 認証フィルタが必ずかかる
5. **権限チェック集約**: Spring Security のアノテーションで一元管理
6. **キャッシュ制御**: 管理 API は `Cache-Control: no-store`
7. **誤アクセス防止**: 顧客トークンで管理 API にアクセスすると 401/403

### 次のステップ

技術設計が完了したので、次は実装タスク（CHG-008）の作成に進みます。
