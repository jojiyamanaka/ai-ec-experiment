# API仕様（UI-バックエンド間）

**目的**: フロントエンド（UI層）とバックエンド間のREST API仕様を定義する
**スコープ**: APIエンドポイント、リクエスト/レスポンス型、エラーコード

**関連ドキュメント**:
- [技術仕様](../SPEC.md) - 技術方針・アーキテクチャ
- [データモデル](../data-model.md) - エンティティ定義
- [商品ドメイン](../specs/product.md) - 商品関連API
- [在庫ドメイン](../specs/inventory.md) - 在庫引当API
- [注文ドメイン](../specs/order.md) - 注文関連API

---

## 概要
AI EC Experiment のバックエンド API 仕様書

ベースURL: `http://localhost:8080/api`

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

## 商品 API

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
    "orderNumber": "ORD-20250210-001",
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
    "orderNumber": "ORD-20250210-001",
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
    "orderNumber": "ORD-20250210-001",
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
    "orderNumber": "ORD-20250210-001",
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
    "orderNumber": "ORD-20250210-001",
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
    "orderNumber": "ORD-20250210-001",
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
      "orderNumber": "ORD-20250210-001",
      "items": [...],
      "totalPrice": 17960,
      "status": "PENDING",
      "createdAt": "2025-02-10T12:00:00Z",
      "updatedAt": "2025-02-10T12:00:00Z"
    },
    {
      "orderId": 2,
      "orderNumber": "ORD-20250210-002",
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
  orderNumber: string     // 注文番号（ORD-YYYYMMDD-XXX形式）
  items: OrderItem[]      // 注文アイテム配列
  totalPrice: number      // 合計金額
  status: string          // 注文状態（PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED）
  createdAt: string       // 作成日時（ISO 8601形式）
  updatedAt: string       // 更新日時（ISO 8601形式）
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
| CART_EMPTY | 400 | カートが空です |
| ORDER_NOT_CANCELLABLE | 400 | この注文はキャンセルできません |
| ALREADY_CANCELLED | 400 | この注文は既にキャンセルされています |
| INVALID_STATUS_TRANSITION | 400 | 不正な状態遷移です |
| INVALID_QUANTITY | 400 | 無効な数量です |
| INVALID_REQUEST | 400 | 無効なリクエストです |
| NO_RESERVATIONS | 400 | 仮引当が存在しません |
| INTERNAL_ERROR | 500 | 内部エラーが発生しました |

### フロントエンドエラー（クライアント側）

| コード | 説明 |
|--------|------|
| NETWORK_ERROR | ネットワークエラーが発生しました |

---

## 注意事項

1. すべてのリクエストには `Content-Type: application/json` ヘッダーが必要です
2. カート関連のAPIは `X-Session-Id` ヘッダーが必須です
3. セッションIDはクライアント側で生成・管理します
4. 商品の在庫数は注文時にチェックされます
5. 注文番号は `ORD-YYYYMMDD-XXX` 形式で自動生成されます
