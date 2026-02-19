# API仕様（UI-BFF間）

**目的**: フロントエンド（UI層）とBFF間のREST API仕様を定義する
**スコープ**: APIエンドポイント、リクエスト/レスポンス型、エラーコード

**関連ドキュメント**:
- [技術仕様](../SPEC.md) - 技術方針・アーキテクチャ
- [データモデル](../data-model.md) - エンティティ定義・型定義
- [BFFアーキテクチャ](../specs/bff-architecture.md) - BFFエンドポイント表・プロキシマッピング
- [Core API OpenAPI仕様](../api/openapi.json) - springdoc-openapi 自動生成の機械可読仕様（正式契約）。本ファイルは手書きの参照用であり、詳細な型定義は openapi.json が正とする

---

## 概要

AI EC Experiment の API 仕様書（Phase 3 / BFF構成）。

ブラウザ（UI）から利用する公開APIベースURL:
- Customer BFF: `http://localhost:3001`
- BackOffice BFF: `http://localhost:3002`

内部API（Core API）: `http://backend:8080`（内部ネットワーク専用、BFF経由でのみアクセス可能）

> **重要**: UI 実装の接続先は [BFFアーキテクチャ](../specs/bff-architecture.md) のエンドポイント表を正とする。
> 以下は Core API の内部仕様であり、BFF が内部的にプロキシする際の参照用。

---

## 共通仕様

### レスポンス形式

**成功時**: `{ "success": true, "data": { ... } }`
**エラー時**: `{ "success": false, "error": { "code": "ERROR_CODE", "message": "エラーメッセージ" } }`

### 共通ヘッダー

| ヘッダー | 用途 | 必須条件 |
|---------|------|---------|
| `Content-Type: application/json` | 全リクエスト | 常に |
| `X-Session-Id: <uuid>` | セッション識別（ゲスト/会員共通） | 全API（BFFが初回生成） |
| `Authorization: Bearer <token>` | 顧客認証 | 認証必須API |
| `Authorization: Bearer <bo_token>` | 管理者認証 | 管理API |

### 共通レスポンス型

型の詳細定義は [データモデル](../data-model.md) を参照。

**Product オブジェクト**: `{ id, name, price, image, description, stock, isPublished }`
**CartItem オブジェクト**: `{ id, product: Product, quantity }`
**Cart オブジェクト**: `{ items: CartItem[], totalQuantity, totalPrice }`
**OrderItem オブジェクト**: `{ product: Product, quantity, subtotal }`
**Order オブジェクト**: `{ orderId, orderNumber, items: OrderItem[], totalPrice, status, createdAt, updatedAt }`
**User オブジェクト**: `{ id, email, displayName, role, createdAt }`
**AuthResponse オブジェクト**: `{ user: User, token, expiresAt }`
**BoUser オブジェクト**: `{ id, email, displayName, permissionLevel, lastLoginAt, isActive, createdAt, updatedAt }`
**BoAuthResponse オブジェクト**: `{ user: BoUser, token, expiresAt }`

---

## BFF 集約エンドポイント（Customer BFF のみ）

Core API には存在せず、BFF が複数 Core API を並列呼び出しして集約して返す。

| Method | Path | 認証 | Response (data) |
|--------|------|------|-----------------|
| GET | `/api/products/:id/full` | 不要 | `{ product: Product, relatedProducts: Product[] }` |
| GET | `/api/orders/:id/full` | User | `{ order: Order, items: [{ ...OrderItem, product: Product }] }` |

---

## Core API エンドポイント一覧

### 商品 API

| # | Method | Path | 認証 | Request | Response (data) | 主なエラー |
|---|--------|------|------|---------|-----------------|-----------|
| 1 | GET | `/api/item` | 不要 | query: `page`, `limit` | `{ items: Product[], total, page, limit }` | — |
| 2 | GET | `/api/item/:id` | 不要 | path: `id` | Product | ITEM_NOT_FOUND |
| 3 | PUT | `/api/item/:id` | 管理者 | body: `{ price?, stock?, isPublished? }` | Product | ITEM_NOT_FOUND |

### カート API

全て `X-Session-Id` ヘッダー必須。

| # | Method | Path | Request | Response (data) | 主なエラー |
|---|--------|------|---------|-----------------|-----------|
| 4 | GET | `/api/order/cart` | — | Cart | — |
| 5 | POST | `/api/order/cart/items` | body: `{ productId, quantity? }` | Cart | ITEM_NOT_FOUND, OUT_OF_STOCK |
| 6 | PUT | `/api/order/cart/items/:id` | body: `{ quantity }` | Cart | OUT_OF_STOCK |
| 7 | DELETE | `/api/order/cart/items/:id` | path: `id` | Cart | — |

### 注文 API

| # | Method | Path | 認証 | Request | Response (data) | 主なエラー |
|---|--------|------|------|---------|-----------------|-----------|
| 8 | POST | `/api/order` | Session | body: `{ cartId }` | Order | CART_EMPTY, OUT_OF_STOCK |
| 9 | GET | `/api/order/:id` | Session | path: `id` | Order | ORDER_NOT_FOUND |
| 10 | POST | `/api/order/:id/cancel` | Session | path: `id` | Order (CANCELLED) | ORDER_NOT_FOUND, ORDER_NOT_CANCELLABLE, ALREADY_CANCELLED |
| 11 | POST | `/api/order/:id/confirm` | 管理者 | path: `id` | Order (CONFIRMED) | INVALID_STATUS_TRANSITION |
| 12 | POST | `/api/order/:id/mark-shipped` | 管理者 | path: `id` | Order (SHIPPED) | INVALID_STATUS_TRANSITION |
| 13 | POST | `/api/order/:id/deliver` | 管理者 | path: `id` | Order (DELIVERED) | INVALID_STATUS_TRANSITION |
| 14 | GET | `/api/order` | 管理者 | — | Order[] | — |

### 顧客認証 API

| # | Method | Path | 認証 | Request | Response (data) | 主なエラー |
|---|--------|------|------|---------|-----------------|-----------|
| 15 | POST | `/api/auth/register` | 不要 | body: `{ email, displayName, password }` | AuthResponse | EMAIL_ALREADY_EXISTS, INVALID_REQUEST |
| 16 | POST | `/api/auth/login` | 不要 | body: `{ email, password }` | AuthResponse | INVALID_CREDENTIALS |
| 17 | POST | `/api/auth/logout` | User | `Authorization: Bearer <token>` | `{ message }` | UNAUTHORIZED |
| 18 | GET | `/api/auth/me` | User | — | User | UNAUTHORIZED |
| 19 | GET | `/api/order/history` | User | — | Order[] | UNAUTHORIZED |

**セキュリティ**: ログイン失敗時、パスワード誤りとアカウント不存在で同じエラーメッセージを返す（アカウント存在判別防止）。

### 管理者認証 API（BoAuth）

| # | Method | Path | 認証 | Request | Response (data) | 主なエラー |
|---|--------|------|------|---------|-----------------|-----------|
| 20 | POST | `/api/bo-auth/login` | 不要 | body: `{ email, password }` | BoAuthResponse | INVALID_CREDENTIALS, BO_USER_INACTIVE |
| 21 | POST | `/api/bo-auth/logout` | BoUser | `Authorization: Bearer <token>` | `{ message }` | — |
| 22 | GET | `/api/bo-auth/me` | BoUser | — | BoUser | INVALID_TOKEN, TOKEN_EXPIRED, TOKEN_REVOKED |

### 管理 API（/api/bo/**）

全て BoUser 認証必須（`Authorization: Bearer <bo_token>`）。

| # | Method | Path | 権限 | Request | Response (data) | 主なエラー |
|---|--------|------|------|---------|-----------------|-----------|
| 23 | GET | `/api/bo/admin/members` | ADMIN+ | — | Member[] | — |
| 24 | GET | `/api/bo/admin/members/:id` | ADMIN+ | path: `id` | Member + orderSummary | — |
| 25 | PUT | `/api/bo/admin/members/:id/status` | ADMIN+ | body: `{ isActive }` | Member | — |
| 26 | GET | `/api/bo/admin/inventory` | ADMIN+ | — | InventoryItem[] | — |
| 27 | GET | `/api/bo/admin/inventory/adjustments` | ADMIN+ | — | AdjustmentResult[] | — |
| 28 | POST | `/api/bo/admin/inventory/adjust` | ADMIN+ | body: `{ productId, adjustment, reason }` | AdjustmentResult | — |
| 29 | GET | `/api/bo/admin/bo-users` | SUPER_ADMIN | — | BoUser[] | — |
| 30 | POST | `/api/bo/admin/bo-users` | SUPER_ADMIN | body: `{ email, displayName, password, permissionLevel }` | BoUser | EMAIL_ALREADY_EXISTS |

**InventoryItem**: `{ productId, productName, physicalStock, availableStock, reservedStock, isPublished }`
**AdjustmentResult**: `{ productId, productName, previousStock, newStock, adjustment, adjustedBy, reason, adjustedAt }`

---

## エラーコード一覧

### バックエンドエラー

| コード | HTTP | 説明 |
|--------|------|------|
| ITEM_NOT_FOUND | 404 | 商品が見つかりません |
| CART_NOT_FOUND | 404 | カートが見つかりません |
| ORDER_NOT_FOUND | 404 | 注文が見つかりません |
| RESERVATION_NOT_FOUND | 404 | 在庫引当が見つかりません |
| BO_USER_NOT_FOUND | 404 | BoUserが見つかりません |
| OUT_OF_STOCK | 409 | 在庫が不足している商品があります |
| INSUFFICIENT_STOCK | 409 | 有効在庫が不足しています |
| EMAIL_ALREADY_EXISTS | 409 | このメールアドレスは既に登録されています |
| ITEM_NOT_AVAILABLE | 400 | 非公開商品へのカート追加・注文確定時 |
| CART_EMPTY | 400 | カートが空です |
| ORDER_NOT_CANCELLABLE | 400 | この注文はキャンセルできません |
| ALREADY_CANCELLED | 400 | この注文は既にキャンセルされています |
| INVALID_STATUS_TRANSITION | 400 | 不正な状態遷移です |
| INVALID_QUANTITY | 400 | 無効な数量です |
| INVALID_REQUEST | 400 | 無効なリクエストです |
| INVALID_CREDENTIALS | 400 | メールアドレスまたはパスワードが正しくありません |
| UNAUTHORIZED | 400 | 認証が必要です |
| NO_RESERVATIONS | 400 | 仮引当が存在しません |
| INVALID_TOKEN | 401 | 無効なトークンです |
| TOKEN_EXPIRED | 401 | トークンの有効期限が切れています |
| TOKEN_REVOKED | 401 | トークンが失効しています |
| BO_USER_INACTIVE | 403 | BoUserが無効化されています |
| FORBIDDEN | 403 | 権限が不足しています |
| CUSTOMER_TOKEN_NOT_ALLOWED | 403 | 顧客トークンでは管理APIにアクセスできません |
| INTERNAL_ERROR | 500 | 内部エラーが発生しました |

### フロントエンドエラー

| コード | 説明 |
|--------|------|
| NETWORK_ERROR | ネットワークエラーが発生しました |

---

## 注意事項

1. すべてのリクエストには `Content-Type: application/json` ヘッダーが必要
2. カート関連のAPIは `X-Session-Id` ヘッダーが必須
3. セッションIDはクライアント側で生成・管理
4. 商品の在庫数は注文時にチェック
5. 注文番号は `ORD-xxxxxxxxxx` 形式で自動生成
6. 管理APIは BoUser 認証が必須。顧客トークンでは 401/403 エラー
7. 管理APIのレスポンスには `Cache-Control: no-store` ヘッダーが付与
