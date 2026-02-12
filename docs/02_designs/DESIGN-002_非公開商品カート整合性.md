# DESIGN-002: 非公開商品のカート整合性

要件: `docs/01_requirements/REQ-002_非公開商品カート整合性.md`
作成日: 2026-02-12

---

## 1. 設計方針

既存の例外処理パターン（`BusinessException` / `GlobalExceptionHandler`）とエラーレスポンス形式（`ApiResponse.error()` / `ApiResponse.errorWithDetails()`）を踏襲する。REQ-001（在庫不足詳細エラー）で導入した `details` 付きエラーレスポンスの仕組みを再利用する。

---

## 2. API設計

### 2-1. カート追加時（REQ 2-1）

**エンドポイント**: `POST /api/order/cart/items`

非公開商品を追加しようとした場合:

```json
{
  "success": false,
  "error": {
    "code": "ITEM_NOT_AVAILABLE",
    "message": "この商品は現在購入できません"
  }
}
```

- HTTPステータス: 400 Bad Request
- 例外: `BusinessException("ITEM_NOT_AVAILABLE", "この商品は現在購入できません")`

### 2-2. 注文確定時（REQ 2-2）

**エンドポイント**: `POST /api/order`

カート内に非公開商品が含まれている場合:

```json
{
  "success": false,
  "error": {
    "code": "ITEM_NOT_AVAILABLE",
    "message": "購入できない商品がカートに含まれています",
    "details": [
      {
        "productId": 3,
        "productName": "非公開になった商品"
      }
    ]
  }
}
```

- HTTPステータス: 400 Bad Request
- 新規例外クラスを作成して `details` を保持する（後述）

### 2-3. カート取得時（REQ 2-3）

**エンドポイント**: `GET /api/order/cart/items`

非公開商品がカート内に存在する場合、レスポンスから除外して返す。副作用として仮引当解放とカートアイテム削除を行う。エラーにはしない（正常レスポンスとして除外済みカートを返す）。

---

## 3. バックエンド実装

### 3-1. 新規: `UnavailableProductDetail.java`（DTO）

非公開商品の詳細情報を保持するDTO。`StockShortageDetail` と同様の役割。

```
package com.example.aiec.dto

フィールド:
- productId: Long
- productName: String
```

### 3-2. 新規: `ItemNotAvailableException.java`（例外）

`BusinessException` を継承し、`List<UnavailableProductDetail>` を保持する。`InsufficientStockException` と同じパターン。

```
package com.example.aiec.exception

- extends BusinessException
- フィールド: List<UnavailableProductDetail> details
- コンストラクタ: (String errorCode, String errorMessage, List<UnavailableProductDetail> details)
```

### 3-3. 変更: `GlobalExceptionHandler.java`

`ItemNotAvailableException` 用のハンドラーを追加。`BusinessException` より先に処理されるよう配置。

- HTTPステータス: 400 Bad Request
- `ApiResponse.errorWithDetails()` を使用

### 3-4. 変更: `CartService.java`

**`addToCart()`** — 商品取得後に公開状態をチェック:

```java
// 既存: productRepository.findById(request.getProductId()) の直後
if (!product.getIsPublished()) {
    throw new BusinessException("ITEM_NOT_AVAILABLE", "この商品は現在購入できません");
}
```

**`getOrCreateCart()`** — カート取得時に非公開商品を除外:

```java
// カート取得後、非公開商品をフィルタ
List<CartItem> unpublishedItems = cart.getItems().stream()
    .filter(item -> !item.getProduct().getIsPublished())
    .toList();

for (CartItem item : unpublishedItems) {
    inventoryService.releaseReservation(sessionId, item.getProduct().getId());
    cart.removeItem(item);
    cartItemRepository.delete(item);
}
// 除外があった場合はカートを保存
```

### 3-5. 変更: `OrderService.java`

**`createOrder()`** — カートアイテム変換前に非公開チェック:

```java
// cart.getItems().isEmpty() チェックの後
List<UnavailableProductDetail> unavailableProducts = cart.getItems().stream()
    .filter(item -> !item.getProduct().getIsPublished())
    .map(item -> new UnavailableProductDetail(
        item.getProduct().getId(),
        item.getProduct().getName()))
    .toList();

if (!unavailableProducts.isEmpty()) {
    throw new ItemNotAvailableException(
        "ITEM_NOT_AVAILABLE",
        "購入できない商品がカートに含まれています",
        unavailableProducts);
}
```

---

## 4. フロントエンド実装

### 4-1. 変更: `types/api.ts`

`ApiError.details` の型を拡張。現在 `StockShortageDetail[]` 固定だが、`UnavailableProductDetail` も含まれ得るため `details?: unknown[]` にするか、ユニオン型にする。

案: エラーコードに応じてフロントで判別するため、`details?: Record<string, unknown>[]` として汎用化。

### 4-2. 変更: `ItemDetailPage.tsx`

`handleAddToCart()` の catch ブロックで `ITEM_NOT_AVAILABLE` エラーをハンドリング。`alert()` の代わりに状態変数でエラーメッセージを表示。

### 4-3. 変更: `OrderConfirmPage.tsx`

既存の `OUT_OF_STOCK` エラー表示ブロックの後に、`ITEM_NOT_AVAILABLE` エラー用の表示を追加。非公開商品名の一覧とカートへの誘導メッセージを表示。

---

## 5. 処理フロー

### カート追加時
```
ユーザー → addToCart API
  → productRepository.findById()
  → isPublished チェック → false → BusinessException (400)
  → (true の場合) 既存処理を続行
```

### カート取得時
```
ユーザー → getCart API
  → カート取得
  → 非公開商品をフィルタ
  → 該当あり → 仮引当解放 + カートアイテム削除
  → 除外済みカートを返却（正常レスポンス）
```

### 注文確定時
```
ユーザー → createOrder API
  → カート取得
  → 非公開商品チェック → あり → ItemNotAvailableException (400, details付き)
  → (なしの場合) 既存処理を続行（仮引当→本引当）
```

---

## 6. 既存パターンとの整合性

| 観点 | REQ-001（在庫不足） | REQ-002（非公開商品） |
|------|---------------------|----------------------|
| 例外クラス | `InsufficientStockException extends ConflictException` | `ItemNotAvailableException extends BusinessException` |
| HTTPステータス | 409 Conflict | 400 Bad Request |
| エラーコード | `OUT_OF_STOCK` | `ITEM_NOT_AVAILABLE` |
| 詳細DTO | `StockShortageDetail` | `UnavailableProductDetail` |
| レスポンス | `ApiResponse.errorWithDetails()` | `ApiResponse.errorWithDetails()` |
| ハンドラー | `GlobalExceptionHandler` に専用メソッド | 同上 |

---

## 7. テスト観点

- カート追加: 非公開商品 → 400エラー、公開商品 → 正常追加
- カート取得: 非公開商品が除外される、仮引当が解放される、合計が再計算される
- 注文確定: 非公開商品含む → 400エラー（details付き）、全商品公開 → 正常注文
- 商品非公開化後: 既存カートの次回取得時に除外される
- 境界: カート内全商品が非公開 → カート空状態で返却
