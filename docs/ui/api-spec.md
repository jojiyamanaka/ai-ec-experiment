# API仕様（UI-BFF間）

**目的**: フロントエンド（UI層）とBFF間のREST API仕様を定義する
**スコープ**: APIエンドポイント、リクエスト/レスポンス型、エラーコード

**関連ドキュメント**:
- [技術仕様](../SPEC.md) - 技術方針・アーキテクチャ
- [データモデル](../data-model.md) - エンティティ定義
- [商品ドメイン](../specs/product.md) - 商品関連API
- [在庫ドメイン](../specs/inventory.md) - 在庫引当API
- [注文ドメイン](../specs/order.md) - 注文関連API

---

## 概要
AI EC Experiment の API 仕様書（Phase 3 / BFF構成）。

ブラウザ（UI）から利用する公開APIベースURL:

- Customer BFF: `http://localhost:3001`
- BackOffice BFF: `http://localhost:3002`

内部API（Core API）ベースURL:

- `http://backend:8080`（内部ネットワーク専用）
- 外部ブラウザからの直接アクセスは不可

> 注記: 本書前半には移行前からの Core API 詳細を含む。UI 実装の接続先は末尾の「API構成（Phase 3完了後）」を正とする。

## 共通レスポンス形式

### 成功時
```json
{
  "success": true,
  "data": { ... }
}
```

### エラー時
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "エラーメッセージ"
  }
}
```

---

## 商品 API（Core API / 内部参照）

### 1. 商品一覧取得
公開されている商品の一覧を取得します。

**エンドポイント**
```
GET /api/item
```

**クエリパラメータ**
| パラメータ | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| page | number | × | ページ番号（デフォルト: 1） |
| limit | number | × | 1ページあたりの件数（デフォルト: 20） |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "name": "ワイヤレスイヤホン",
        "price": 8980,
        "image": "https://placehold.co/400x300/3b82f6/ffffff?text=Product+1",
        "description": "高音質で長時間バッテリー対応のワイヤレスイヤホン",
        "stock": 12,
        "isPublished": true
      }
    ],
    "total": 8,
    "page": 1,
    "limit": 20
  }
}
```

---

### 2. 商品詳細取得
指定した商品の詳細情報を取得します。

**エンドポイント**
```
GET /api/item/:id
```

**パスパラメータ**
| パラメータ | 型 | 説明 |
|-----------|-----|------|
| id | number | 商品ID |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "ワイヤレスイヤホン",
    "price": 8980,
    "image": "https://placehold.co/400x300/3b82f6/ffffff?text=Product+1",
    "description": "高音質で長時間バッテリー対応のワイヤレスイヤホン",
    "stock": 12,
    "isPublished": true
  }
}
```

**エラーレスポンス**
```json
{
  "success": false,
  "error": {
    "code": "ITEM_NOT_FOUND",
    "message": "商品が見つかりません"
  }
}
```

---

### 3. 商品更新（管理用）
商品情報を更新します。（管理画面用）

**エンドポイント**
```
PUT /api/item/:id
```

**パスパラメータ**
| パラメータ | 型 | 説明 |
|-----------|-----|------|
| id | number | 商品ID |

**リクエストボディ**
```json
{
  "price": 8980,
  "stock": 12,
  "isPublished": true
}
```

**リクエストフィールド**
| フィールド | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| price | number | × | 価格 |
| stock | number | × | 在庫数 |
| isPublished | boolean | × | 公開状態 |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "ワイヤレスイヤホン",
    "price": 8980,
    "image": "https://placehold.co/400x300/3b82f6/ffffff?text=Product+1",
    "description": "高音質で長時間バッテリー対応のワイヤレスイヤホン",
    "stock": 12,
    "isPublished": true
  }
}
```

---

## カート API

### 4. カート取得
現在のカート情報を取得します。

**エンドポイント**
```
GET /api/order/cart
```

**ヘッダー**
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "product": {
          "id": 1,
          "name": "ワイヤレスイヤホン",
          "price": 8980,
          "image": "https://placehold.co/400x300/3b82f6/ffffff?text=Product+1",
          "description": "高音質で長時間バッテリー対応のワイヤレスイヤホン",
          "stock": 12,
          "isPublished": true
        },
        "quantity": 2
      }
    ],
    "totalQuantity": 2,
    "totalPrice": 17960
  }
}
```

---

### 5. カートに商品追加
カートに商品を追加します。既に存在する商品の場合は数量が増加します。

**エンドポイント**
```
POST /api/order/cart/items
```

**ヘッダー**
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**リクエストボディ**
```json
{
  "productId": 1,
  "quantity": 1
}
```

**リクエストフィールド**
| フィールド | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| productId | number | ○ | 商品ID |
| quantity | number | × | 数量（デフォルト: 1） |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "product": {
          "id": 1,
          "name": "ワイヤレスイヤホン",
          "price": 8980,
          "image": "https://placehold.co/400x300/3b82f6/ffffff?text=Product+1",
          "description": "高音質で長時間バッテリー対応のワイヤレスイヤホン",
          "stock": 12,
          "isPublished": true
        },
        "quantity": 1
      }
    ],
    "totalQuantity": 1,
    "totalPrice": 8980
  }
}
```

**エラーレスポンス**
```json
{
  "success": false,
  "error": {
    "code": "ITEM_NOT_FOUND",
    "message": "商品が見つかりません"
  }
}
```

```json
{
  "success": false,
  "error": {
    "code": "OUT_OF_STOCK",
    "message": "在庫が不足しています"
  }
}
```

---

### 6. カート内商品の数量変更
カート内の商品の数量を変更します。

**エンドポイント**
```
PUT /api/order/cart/items/:id
```

**ヘッダー**
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**パスパラメータ**
| パラメータ | 型 | 説明 |
|-----------|-----|------|
| id | number | カートアイテムID（商品IDと同じ） |

**リクエストボディ**
```json
{
  "quantity": 3
}
```

**リクエストフィールド**
| フィールド | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| quantity | number | ○ | 新しい数量 |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": 1,
        "product": {
          "id": 1,
          "name": "ワイヤレスイヤホン",
          "price": 8980,
          "image": "https://placehold.co/400x300/3b82f6/ffffff?text=Product+1",
          "description": "高音質で長時間バッテリー対応のワイヤレスイヤホン",
          "stock": 12,
          "isPublished": true
        },
        "quantity": 3
      }
    ],
    "totalQuantity": 3,
    "totalPrice": 26940
  }
}
```

---

### 7. カートから商品削除
カートから商品を削除します。

**エンドポイント**
```
DELETE /api/order/cart/items/:id
```

**ヘッダー**
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**パスパラメータ**
| パラメータ | 型 | 説明 |
|-----------|-----|------|
| id | number | カートアイテムID（商品IDと同じ） |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "items": [],
    "totalQuantity": 0,
    "totalPrice": 0
  }
}
```

---

## 注文 API

### 8. 注文作成
カートの内容を元に注文を作成します。

**エンドポイント**
```
POST /api/order
```

**ヘッダー**
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**リクエストボディ**
```json
{
  "cartId": "session-cart-id"
}
```

**リクエストフィールド**
| フィールド | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| cartId | string | ○ | カートID（セッションIDと同じ） |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-0000000001",
    "items": [
      {
        "product": {
          "id": 1,
          "name": "ワイヤレスイヤホン",
          "price": 8980,
          "image": "https://placehold.co/400x300/3b82f6/ffffff?text=Product+1",
          "description": "高音質で長時間バッテリー対応のワイヤレスイヤホン",
          "stock": 12,
          "isPublished": true
        },
        "quantity": 2,
        "subtotal": 17960
      }
    ],
    "totalPrice": 17960,
    "status": "PENDING",
    "createdAt": "2025-02-10T12:00:00Z"
  }
}
```

**エラーレスポンス**
```json
{
  "success": false,
  "error": {
    "code": "CART_EMPTY",
    "message": "カートが空です"
  }
}
```

```json
{
  "success": false,
  "error": {
    "code": "OUT_OF_STOCK",
    "message": "在庫が不足している商品があります",
    "details": [
      {
        "productId": 1,
        "productName": "ワイヤレスイヤホン",
        "requestedQuantity": 20,
        "availableStock": 12
      }
    ]
  }
}
```

---

### 9. 注文詳細取得
注文の詳細情報を取得します。

**エンドポイント**
```
GET /api/order/:id
```

**ヘッダー**
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**パスパラメータ**
| パラメータ | 型 | 説明 |
|-----------|-----|------|
| id | number | 注文ID |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-0000000001",
    "items": [
      {
        "product": {
          "id": 1,
          "name": "ワイヤレスイヤホン",
          "price": 8980,
          "image": "https://placehold.co/400x300/3b82f6/ffffff?text=Product+1",
          "description": "高音質で長時間バッテリー対応のワイヤレスイヤホン",
          "stock": 12,
          "isPublished": true
        },
        "quantity": 2,
        "subtotal": 17960
      }
    ],
    "totalPrice": 17960,
    "status": "PENDING",
    "createdAt": "2025-02-10T12:00:00Z",
    "updatedAt": "2025-02-10T12:00:00Z"
  }
}
```

**エラーレスポンス**
```json
{
  "success": false,
  "error": {
    "code": "ORDER_NOT_FOUND",
    "message": "注文が見つかりません"
  }
}
```

---

### 10. 注文キャンセル（顧客向け）
注文をキャンセルします。在庫が戻ります。

**エンドポイント**
```
POST /api/order/:id/cancel
```

**ヘッダー**
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**パスパラメータ**
| パラメータ | 型 | 説明 |
|-----------|-----|------|
| id | number | 注文ID |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-0000000001",
    "items": [...],
    "totalPrice": 17960,
    "status": "CANCELLED",
    "createdAt": "2025-02-10T12:00:00Z",
    "updatedAt": "2025-02-10T12:05:00Z"
  }
}
```

**エラーレスポンス**
```json
{
  "success": false,
  "error": {
    "code": "ORDER_NOT_FOUND",
    "message": "注文が見つかりません"
  }
}
```

```json
{
  "success": false,
  "error": {
    "code": "ORDER_NOT_CANCELLABLE",
    "message": "この注文はキャンセルできません"
  }
}
```

```json
{
  "success": false,
  "error": {
    "code": "ALREADY_CANCELLED",
    "message": "この注文は既にキャンセルされています"
  }
}
```

---

### 11. 注文確認（管理者向け）
注文を確認します（PENDING → CONFIRMED）。

**エンドポイント**
```
POST /api/order/:id/confirm
```

**パスパラメータ**
| パラメータ | 型 | 説明 |
|-----------|-----|------|
| id | number | 注文ID |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-0000000001",
    "items": [...],
    "totalPrice": 17960,
    "status": "CONFIRMED",
    "createdAt": "2025-02-10T12:00:00Z",
    "updatedAt": "2025-02-10T12:10:00Z"
  }
}
```

**エラーレスポンス**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_STATUS_TRANSITION",
    "message": "この注文は確認できません（現在のステータス: SHIPPED）"
  }
}
```

---

### 12. 注文発送（管理者向け）
注文を発送します（CONFIRMED → SHIPPED）。

**エンドポイント**
```
POST /api/order/:id/ship
```

**パスパラメータ**
| パラメータ | 型 | 説明 |
|-----------|-----|------|
| id | number | 注文ID |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-0000000001",
    "items": [...],
    "totalPrice": 17960,
    "status": "SHIPPED",
    "createdAt": "2025-02-10T12:00:00Z",
    "updatedAt": "2025-02-10T12:15:00Z"
  }
}
```

---

### 13. 注文配達完了（管理者向け）
注文を配達完了にします（SHIPPED → DELIVERED）。

**エンドポイント**
```
POST /api/order/:id/deliver
```

**パスパラメータ**
| パラメータ | 型 | 説明 |
|-----------|-----|------|
| id | number | 注文ID |

**レスポンス**
```json
{
  "success": true,
  "data": {
    "orderId": 1,
    "orderNumber": "ORD-0000000001",
    "items": [...],
    "totalPrice": 17960,
    "status": "DELIVERED",
    "createdAt": "2025-02-10T12:00:00Z",
    "updatedAt": "2025-02-10T13:00:00Z"
  }
}
```

---

### 14. 全注文取得（管理者向け）
すべての注文を取得します。

**エンドポイント**
```
GET /api/order
```

**レスポンス**
```json
{
  "success": true,
  "data": [
    {
      "orderId": 1,
      "orderNumber": "ORD-0000000001",
      "items": [...],
      "totalPrice": 17960,
      "status": "PENDING",
      "createdAt": "2025-02-10T12:00:00Z",
      "updatedAt": "2025-02-10T12:00:00Z"
    },
    {
      "orderId": 2,
      "orderNumber": "ORD-0000000002",
      "items": [...],
      "totalPrice": 25000,
      "status": "CONFIRMED",
      "createdAt": "2025-02-10T13:00:00Z",
      "updatedAt": "2025-02-10T13:10:00Z"
    }
  ]
}
```

---

## データモデル

### Product（商品）
```typescript
{
  id: number              // 商品ID
  name: string            // 商品名
  price: number           // 価格（円）
  image: string           // 商品画像URL
  description: string     // 商品説明
  stock: number           // 在庫数
  isPublished: boolean    // 公開状態
}
```

### CartItem（カートアイテム）
```typescript
{
  id: number              // カートアイテムID（商品IDと同じ）
  product: Product        // 商品情報
  quantity: number        // 数量
}
```

### Cart（カート）
```typescript
{
  items: CartItem[]       // カートアイテム配列
  totalQuantity: number   // 総数量
  totalPrice: number      // 合計金額
}
```

### OrderItem（注文アイテム）
```typescript
{
  product: Product        // 商品情報
  quantity: number        // 数量
  subtotal: number        // 小計
}
```

### Order（注文）
```typescript
{
  orderId: number         // 注文ID
  orderNumber: string     // 注文番号（ORD-xxxxxxxxxx形式）
  items: OrderItem[]      // 注文アイテム配列
  totalPrice: number      // 合計金額
  status: string          // 注文状態（PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED）
  createdAt: string       // 作成日時（ISO 8601形式）
  updatedAt: string       // 更新日時（ISO 8601形式）
}
```

---

## 認証 API

### 15. 会員登録
会員アカウントを新規作成します。登録と同時にログイン状態になります。

**エンドポイント**
```
POST /api/auth/register
```

**リクエスト**
```json
{
  "email": "user@example.com",
  "displayName": "山田太郎",
  "password": "SecurePass123"
}
```

**パラメータ**
| フィールド | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| email | string | ○ | メールアドレス（ログインID） |
| displayName | string | ○ | 表示名（最大100文字） |
| password | string | ○ | パスワード（8文字以上推奨） |

**レスポンス（成功）**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "displayName": "山田太郎",
      "role": "CUSTOMER",
      "createdAt": "2026-02-12T10:00:00Z"
    },
    "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "expiresAt": "2026-02-19T10:00:00Z"
  }
}
```

**エラーレスポンス**
```json
// メールアドレス重複（409 Conflict）
{
  "success": false,
  "error": {
    "code": "EMAIL_ALREADY_EXISTS",
    "message": "このメールアドレスは既に登録されています"
  }
}

// バリデーションエラー（400 Bad Request）
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "メールアドレスの形式が正しくありません"
  }
}
```

---

### 16. ログイン
メールアドレスとパスワードで認証し、トークンを発行します。

**エンドポイント**
```
POST /api/auth/login
```

**リクエスト**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**パラメータ**
| フィールド | 型 | 必須 | 説明 |
|-----------|-----|------|------|
| email | string | ○ | メールアドレス |
| password | string | ○ | パスワード |

**レスポンス（成功）**
```json
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "displayName": "山田太郎",
      "role": "CUSTOMER",
      "createdAt": "2026-02-12T10:00:00Z"
    },
    "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "expiresAt": "2026-02-19T10:00:00Z"
  }
}
```

**エラーレスポンス**
```json
// 認証失敗（400 Bad Request）
{
  "success": false,
  "error": {
    "code": "INVALID_CREDENTIALS",
    "message": "メールアドレスまたはパスワードが正しくありません"
  }
}
```

**セキュリティ**:
- パスワード誤りとアカウント不存在で同じエラーメッセージを返す（アカウント存在判別防止）

---

### 17. ログアウト
認証トークンを失効させます。

**エンドポイント**
```
POST /api/auth/logout
```

**ヘッダー**
```
Authorization: Bearer <token>
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

**エラーレスポンス**
```json
// 認証エラー（400 Bad Request）
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "認証が必要です"
  }
}
```

---

### 18. 会員情報取得
ログイン中の会員情報を取得します。

**エンドポイント**
```
GET /api/auth/me
```

**ヘッダー**
```
Authorization: Bearer <token>
```

**レスポンス（成功）**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "displayName": "山田太郎",
    "role": "CUSTOMER",
    "createdAt": "2026-02-12T10:00:00Z"
  }
}
```

**エラーレスポンス**
```json
// 認証エラー（400 Bad Request）
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "認証が必要です"
  }
}

// トークン有効期限切れ（400 Bad Request）
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "トークンの有効期限が切れています"
  }
}

// トークン失効済み（400 Bad Request）
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "トークンが失効しています"
  }
}
```

---

### 19. 会員注文履歴取得
ログイン中の会員の注文履歴を取得します。

**エンドポイント**
```
GET /api/order/history
```

**ヘッダー**
```
Authorization: Bearer <token>
```

**レスポンス（成功）**
```json
{
  "success": true,
  "data": [
    {
      "orderId": 3,
      "orderNumber": "ORD-0000000003",
      "items": [
        {
          "product": {
            "id": 1,
            "name": "オーガニックマンゴー",
            "price": 1000,
            "image": "/images/mango.jpg"
          },
          "quantity": 2,
          "subtotal": 2000
        }
      ],
      "totalPrice": 2000,
      "status": "DELIVERED",
      "createdAt": "2026-02-10T10:00:00Z",
      "updatedAt": "2026-02-11T15:00:00Z"
    }
  ]
}
```

**仕様**:
- 認証必須
- 自分の注文のみ取得（`userId`で絞り込み）
- 作成日時降順でソート
- 全ステータスの注文を取得

**エラーレスポンス**
```json
// 認証エラー（400 Bad Request）
{
  "success": false,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "認証が必要です"
  }
}
```

---

## 型定義

### User（会員）
```typescript
{
  id: number              // 会員ID
  email: string           // メールアドレス
  displayName: string     // 表示名
  role: string            // ロール（CUSTOMER, ADMIN）
  createdAt: string       // 作成日時（ISO 8601形式）
}
```

### AuthResponse（認証レスポンス）
```typescript
{
  user: User              // 会員情報
  token: string           // 認証トークン（UUID v4、36文字）
  expiresAt: string       // トークン有効期限（ISO 8601形式）
}
```

### BoUser（管理者ユーザー）
```typescript
{
  id: number              // BoUser ID
  email: string           // メールアドレス
  displayName: string     // 表示名
  permissionLevel: string // 権限レベル（SUPER_ADMIN, ADMIN, OPERATOR）
  lastLoginAt: string     // 最終ログイン日時（ISO 8601形式）
  isActive: boolean       // 有効/無効フラグ
  createdAt: string       // 作成日時（ISO 8601形式）
  updatedAt: string       // 更新日時（ISO 8601形式）
}
```

### BoAuthResponse（管理者認証レスポンス）
```typescript
{
  user: BoUser            // 管理者情報
  token: string           // 認証トークン（UUID v4、36文字）
  expiresAt: string       // トークン有効期限（ISO 8601形式）
}
```

---

## BoAuth API（管理者認証）

### 20. BoUser ログイン
管理者アカウントでログインします。

**エンドポイント**
```
POST /api/bo-auth/login
```

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
      "lastLoginAt": "2026-02-13T10:00:00",
      "isActive": true,
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-02-13T10:00:00"
    },
    "token": "550e8400-e29b-41d4-a716-446655440000",
    "expiresAt": "2026-02-20T10:00:00"
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

```json
{
  "success": false,
  "error": {
    "code": "BO_USER_INACTIVE",
    "message": "このアカウントは無効化されています"
  }
}
```

---

### 21. BoUser ログアウト
管理者アカウントからログアウトします（トークンを失効）。

**エンドポイント**
```
POST /api/bo-auth/logout
```

**ヘッダー**
```
Authorization: Bearer <token>
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

---

### 22. BoUser 情報取得
現在ログイン中の管理者情報を取得します。

**エンドポイント**
```
GET /api/bo-auth/me
```

**ヘッダー**
```
Authorization: Bearer <token>
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
    "lastLoginAt": "2026-02-13T10:00:00",
    "isActive": true,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-02-13T10:00:00"
  }
}
```

**レスポンス（エラー）**
```json
{
  "success": false,
  "error": {
    "code": "INVALID_TOKEN",
    "message": "無効なトークンです"
  }
}
```

```json
{
  "success": false,
  "error": {
    "code": "TOKEN_EXPIRED",
    "message": "トークンの有効期限が切れています"
  }
}
```

```json
{
  "success": false,
  "error": {
    "code": "TOKEN_REVOKED",
    "message": "このトークンは失効しています"
  }
}
```

---

## 管理 API（/api/bo/**）

**重要**: すべての管理APIは BoUser 認証が必須です。認証ヘッダー `Authorization: Bearer <bo_token>` が必要です。

### 23. 会員一覧取得（管理者用）
全会員の一覧を取得します。

**エンドポイント**
```
GET /api/bo/admin/members
```

**ヘッダー**
```
Authorization: Bearer <bo_token>
```

**権限**: ADMIN または SUPER_ADMIN

**レスポンス（成功）**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "email": "customer@example.com",
      "displayName": "顧客太郎",
      "isActive": true,
      "createdAt": "2026-01-01T00:00:00",
      "updatedAt": "2026-01-01T00:00:00"
    }
  ]
}
```

---

### 24. 会員詳細取得（管理者用）
特定会員の詳細情報と注文サマリーを取得します。

**エンドポイント**
```
GET /api/bo/admin/members/:id
```

**ヘッダー**
```
Authorization: Bearer <bo_token>
```

**権限**: ADMIN または SUPER_ADMIN

**レスポンス（成功）**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "displayName": "顧客太郎",
    "isActive": true,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-01-01T00:00:00",
    "orderSummary": {
      "totalOrders": 5,
      "totalAmount": 150000
    }
  }
}
```

---

### 25. 会員状態変更（管理者用）
会員アカウントの有効/無効を切り替えます。

**エンドポイント**
```
PUT /api/bo/admin/members/:id/status
```

**ヘッダー**
```
Authorization: Bearer <bo_token>
```

**権限**: ADMIN または SUPER_ADMIN

**リクエスト**
```json
{
  "isActive": false
}
```

**レスポンス（成功）**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "customer@example.com",
    "displayName": "顧客太郎",
    "isActive": false,
    "createdAt": "2026-01-01T00:00:00",
    "updatedAt": "2026-02-13T10:00:00"
  }
}
```

---

### 26. 在庫一覧取得（管理者用）
全商品の在庫情報を取得します。

**エンドポイント**
```
GET /api/bo/admin/inventory
```

**ヘッダー**
```
Authorization: Bearer <bo_token>
```

**権限**: ADMIN または SUPER_ADMIN

**レスポンス（成功）**
```json
{
  "success": true,
  "data": [
    {
      "productId": 1,
      "productName": "ワイヤレスイヤホン",
      "physicalStock": 12,
      "availableStock": 10,
      "reservedStock": 2,
      "isPublished": true
    }
  ]
}
```

---

### 27. 在庫調整（管理者用）
商品の実在庫を調整します。

**エンドポイント**
```
POST /api/bo/admin/inventory/adjust
```

**ヘッダー**
```
Authorization: Bearer <bo_token>
```

**権限**: ADMIN または SUPER_ADMIN

**リクエスト**
```json
{
  "productId": 1,
  "adjustment": 10,
  "reason": "入荷"
}
```

**レスポンス（成功）**
```json
{
  "success": true,
  "data": {
    "productId": 1,
    "productName": "ワイヤレスイヤホン",
    "previousStock": 12,
    "newStock": 22,
    "adjustment": 10,
    "adjustedBy": "admin@example.com",
    "reason": "入荷",
    "adjustedAt": "2026-02-13T10:00:00"
  }
}
```

---

## エラーコード一覧

### バックエンドエラー（HTTPステータス: 400, 404, 409）

| コード | HTTPステータス | 説明 |
|--------|---------------|------|
| ITEM_NOT_FOUND | 404 | 商品が見つかりません |
| CART_NOT_FOUND | 404 | カートが見つかりません |
| ORDER_NOT_FOUND | 404 | 注文が見つかりません |
| RESERVATION_NOT_FOUND | 404 | 在庫引当が見つかりません |
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
| BO_USER_NOT_FOUND | 404 | BoUserが見つかりません |
| BO_USER_INACTIVE | 403 | BoUserが無効化されています |
| INVALID_TOKEN | 401 | 無効なトークンです |
| TOKEN_EXPIRED | 401 | トークンの有効期限が切れています |
| TOKEN_REVOKED | 401 | トークンが失効しています |
| FORBIDDEN | 403 | 権限が不足しています |
| CUSTOMER_TOKEN_NOT_ALLOWED | 403 | 顧客トークンでは管理APIにアクセスできません |
| INTERNAL_ERROR | 500 | 内部エラーが発生しました |

### フロントエンドエラー（クライアント側）

| コード | 説明 |
|--------|------|
| NETWORK_ERROR | ネットワークエラーが発生しました |

---

## 注意事項

1. すべてのリクエストには `Content-Type: application/json` ヘッダーが必要です
2. カート関連のAPIは `X-Session-Id` ヘッダーが必須です
3. 認証が必要なAPIは `Authorization: Bearer <token>` ヘッダーが必須です
   - 顧客向けAPI（例: `/api/products`, `/api/cart`, `/api/orders`）: `authToken` を使用
   - 管理者向けAPI（例: `/api/bo-auth/**`, `/api/admin/**`, `/api/order/**`, `/api/inventory/**`）: `bo_token` を使用
4. セッションIDはクライアント側で生成・管理します
5. 商品の在庫数は注文時にチェックされます
6. 注文番号は `ORD-xxxxxxxxxx` 形式で自動生成されます
7. 管理API（`/api/bo-auth/**`, `/api/admin/**`, `/api/order/**`, `/api/inventory/**`）は BoUser 認証が必須です。顧客トークンでアクセスすると 401/403 エラーが返されます
8. 管理APIのレスポンスには `Cache-Control: no-store` ヘッダーが付与されます（キャッシュ無効化）

## API構成（Phase 3完了後）

### Customer BFF（顧客向け）

**ベースURL**: `http://localhost:3001`

| エンドポイント | メソッド | 説明 | 認証 |
|--------------|---------|------|------|
| /health | GET | ヘルスチェック | 不要 |
| /api/products | GET | 商品一覧取得 | 不要 |
| /api/products/:id | GET | 商品詳細取得 | 不要 |
| /api/cart | GET | カート取得 | User |
| /api/cart/items | POST | カート追加 | User |
| /api/cart/items/:id | PUT | カート数量変更 | User |
| /api/cart/items/:id | DELETE | カート商品削除 | User |
| /api/auth/register | POST | 会員登録 | 不要 |
| /api/auth/login | POST | 会員ログイン | 不要 |
| /api/auth/logout | POST | 会員ログアウト | User |
| /api/members/me | GET | 会員情報取得 | User |
| /api/orders | POST | 注文確定 | User |
| /api/orders | GET | 注文一覧（会員） | User |
| /api/orders/history | GET | 注文履歴 | User |
| /api/orders/:id | GET | 注文詳細 | User |
| /api/orders/:id/cancel | POST | 注文キャンセル | User |

（詳細は技術設計ドキュメント参照）

### BackOffice BFF（管理向け）

**ベースURL**: `http://localhost:3002`

| エンドポイント | メソッド | 説明 | 認証 |
|--------------|---------|------|------|
| /health | GET | ヘルスチェック | 不要 |
| /api/bo-auth/login | POST | 管理ログイン | 不要 |
| /api/bo-auth/logout | POST | 管理ログアウト | BoUser |
| /api/inventory | GET | 在庫一覧取得 | BoUser |
| /api/inventory/adjustments | GET | 在庫調整履歴 | BoUser |
| /api/inventory/adjust | POST | 在庫調整 | BoUser |
| /api/inventory/:id | PUT | 在庫更新 | BoUser |
| /api/admin/orders | GET | 注文一覧取得 | BoUser |
| /api/admin/orders/:id | GET | 注文詳細取得 | BoUser |
| /api/admin/orders/:id | PUT | 注文更新 | BoUser |
| /api/admin/orders/:id/confirm | POST | 注文確認 | BoUser |
| /api/admin/orders/:id/ship | POST | 注文発送 | BoUser |
| /api/admin/orders/:id/deliver | POST | 配達完了 | BoUser |
| /api/admin/orders/:id/cancel | POST | 注文キャンセル | BoUser |
| /api/order | GET | 注文一覧取得（互換エイリアス） | BoUser |
| /api/order/:id | GET | 注文詳細取得（互換エイリアス） | BoUser |
| /api/order/:id | PUT | 注文更新（互換エイリアス） | BoUser |
| /api/order/:id/confirm | POST | 注文確認（互換エイリアス） | BoUser |
| /api/order/:id/ship | POST | 注文発送（互換エイリアス） | BoUser |
| /api/order/:id/deliver | POST | 配達完了（互換エイリアス） | BoUser |
| /api/order/:id/cancel | POST | 注文キャンセル（互換エイリアス） | BoUser |
| /api/admin/members | GET | 会員一覧取得 | BoUser |
| /api/admin/members/:id | GET | 会員詳細取得 | BoUser |
| /api/admin/members/:id/status | PUT | 会員状態更新 | BoUser |
| /api/admin/bo-users | GET | BoUser一覧取得 | BoUser |
| /api/admin/bo-users | POST | BoUser作成 | BoUser |

（詳細は技術設計ドキュメント参照）

### Core API（内部専用）

**ベースURL**: `http://backend:8080`（内部ネットワークのみ）

- 外部からの直接アクセスは不可
- BFF経由でのみアクセス可能
