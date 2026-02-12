# TASK-002: 非公開商品のカート整合性

要件: `docs/01_requirements/REQ-002_非公開商品カート整合性.md`
設計: `docs/02_designs/DESIGN-002_非公開商品カート整合性.md`
作成日: 2026-02-12

検証コマンド:
- バックエンド: `docker compose exec backend ./mvnw compile`
- フロントエンド: `docker compose exec frontend npm run build`
- コンテナ未起動の場合: `docker compose up -d` を先に実行

---

## タスク一覧

### バックエンド

- [ ] **T-1**: `UnavailableProductDetail.java` を新規作成

  パス: `backend/src/main/java/com/example/aiec/dto/UnavailableProductDetail.java`

  参考実装: `backend/src/main/java/com/example/aiec/dto/StockShortageDetail.java` と同じ構成

  ```java
  package com.example.aiec.dto;

  import lombok.AllArgsConstructor;
  import lombok.Data;
  import lombok.NoArgsConstructor;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class UnavailableProductDetail {
      private Long productId;
      private String productName;
  }
  ```

---

- [ ] **T-2**: `ItemNotAvailableException.java` を新規作成

  パス: `backend/src/main/java/com/example/aiec/exception/ItemNotAvailableException.java`

  参考実装: `backend/src/main/java/com/example/aiec/exception/InsufficientStockException.java` と同じパターン

  - `BusinessException` を継承（※ `InsufficientStockException` は `ConflictException` 継承だが、今回は 400 なので `BusinessException`）
  - `List<UnavailableProductDetail> details` フィールドを `@Getter` で公開
  - コンストラクタ: `(String errorCode, String errorMessage, List<UnavailableProductDetail> details)`

---

- [ ] **T-3**: `GlobalExceptionHandler.java` にハンドラーを追加

  パス: `backend/src/main/java/com/example/aiec/exception/GlobalExceptionHandler.java`

  `handleBusinessException()` メソッドの **上** に以下を追加（サブクラスを先に処理させるため）:

  ```java
  @ExceptionHandler(ItemNotAvailableException.class)
  public ResponseEntity<ApiResponse<Void>> handleItemNotAvailableException(ItemNotAvailableException ex) {
      return ResponseEntity
              .status(HttpStatus.BAD_REQUEST)
              .body(ApiResponse.errorWithDetails(ex.getErrorCode(), ex.getErrorMessage(), ex.getDetails()));
  }
  ```

  参考: 同ファイル内の `handleInsufficientStockException()` (43-48行目) と同じ構成。

---

- [ ] **T-4**: `CartService.addToCart()` に公開状態チェックを追加

  パス: `backend/src/main/java/com/example/aiec/service/CartService.java`

  `addToCart()` メソッド内、商品取得（50-51行目）の直後、仮引当作成（54行目）の前に追加:

  ```java
  if (!product.getIsPublished()) {
      throw new BusinessException("ITEM_NOT_AVAILABLE", "この商品は現在購入できません");
  }
  ```

---

- [ ] **T-5**: `CartService.getOrCreateCart()` に非公開商品の自動除外を追加

  パス: `backend/src/main/java/com/example/aiec/service/CartService.java`

  `getOrCreateCart()` メソッド内、`CartDto.fromEntity(cart)` を返す前に追加:

  ```java
  // 非公開商品をカートから除外
  List<CartItem> unpublishedItems = cart.getItems().stream()
          .filter(item -> !item.getProduct().getIsPublished())
          .toList();

  if (!unpublishedItems.isEmpty()) {
      for (CartItem item : unpublishedItems) {
          inventoryService.releaseReservation(sessionId, item.getProduct().getId());
          cart.removeItem(item);
          cartItemRepository.delete(item);
      }
      cart.setUpdatedAt(LocalDateTime.now());
      cartRepository.save(cart);
  }
  ```

  `CartItem` と `List` の import を確認すること。

---

- [ ] **T-6**: `OrderService.createOrder()` に非公開商品チェックを追加

  パス: `backend/src/main/java/com/example/aiec/service/OrderService.java`

  `createOrder()` メソッド内、カート空チェック（44-46行目）の後、注文作成（49行目）の前に追加:

  ```java
  // 非公開商品チェック
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

  import 追加: `UnavailableProductDetail`, `ItemNotAvailableException`, `List`

  エラーレスポンス形式:
  ```json
  {
    "success": false,
    "error": {
      "code": "ITEM_NOT_AVAILABLE",
      "message": "購入できない商品がカートに含まれています",
      "details": [
        { "productId": 3, "productName": "非公開になった商品" }
      ]
    }
  }
  ```

---

### フロントエンド

- [ ] **T-7**: `types/api.ts` に型を追加

  パス: `frontend/src/types/api.ts`

  `StockShortageDetail` の下に追加:

  ```typescript
  export interface UnavailableProductDetail {
    productId: number
    productName: string
  }
  ```

  `ApiError.details` の型を拡張:

  ```typescript
  export interface ApiError {
    code: string
    message: string
    details?: StockShortageDetail[] | UnavailableProductDetail[]
  }
  ```

---

- [ ] **T-8**: `ItemDetailPage.tsx` のエラーハンドリング改善

  パス: `frontend/src/pages/ItemDetailPage.tsx`

  変更内容:
  1. エラー用の state を追加: `const [error, setError] = useState<string | null>(null)`
  2. `handleAddToCart()` の catch ブロックで `alert()` を削除し、`setError()` でメッセージを設定
  3. `addToCart` の戻り値（`ApiResponse`）からエラーを取得してハンドリング
  4. ボタンの上にエラーメッセージ表示エリアを追加

  参考: `OrderConfirmPage.tsx` の 158-198行目（エラー表示ブロック）の UI パターンを参考にする。ただし簡易版でよい（赤背景 + メッセージテキスト）。

---

- [ ] **T-9**: `OrderConfirmPage.tsx` に非公開商品エラー表示を追加

  パス: `frontend/src/pages/OrderConfirmPage.tsx`

  177行目の `{error.code === 'OUT_OF_STOCK' && ...}` ブロックの後に追加:

  ```tsx
  {error.code === 'ITEM_NOT_AVAILABLE' && error.details && error.details.length > 0 && (
    <div className="mt-2">
      <p className="text-sm text-red-700">以下の商品は現在購入できません：</p>
      <ul className="mt-2 space-y-1 text-sm text-red-700">
        {(error.details as UnavailableProductDetail[]).map((detail) => (
          <li key={detail.productId} className="flex items-center">
            <span className="font-medium">{detail.productName}</span>
          </li>
        ))}
      </ul>
      <p className="mt-2 text-sm text-red-700">
        カートに戻って該当商品を削除してください。
      </p>
    </div>
  )}
  ```

  import 追加: `UnavailableProductDetail` を `types/api.ts` から。

---

### ドキュメント更新

- [ ] **T-10**: `docs/ui/api-spec.md` のエラーコード一覧に `ITEM_NOT_AVAILABLE` を追加
  - コード: `ITEM_NOT_AVAILABLE`
  - HTTPステータス: 400
  - 説明: 非公開商品へのカート追加・注文確定時

- [ ] **T-11**: `docs/spec-implementation-gaps.md` の M-3 ステータスを「実装完了」に更新

---

### テスト

- [ ] **T-12**: 手動テストで動作確認
  1. 商品を非公開にした状態でカート追加 → `ITEM_NOT_AVAILABLE` エラー（400）
  2. カートに商品を追加した後、管理画面で非公開に変更 → カート取得時に自動除外
  3. 手順2の後、注文確定 → `ITEM_NOT_AVAILABLE` エラー（400、details付き）
  4. フロントエンドでエラーメッセージが適切に表示される
  5. 公開商品のみのカート → 通常通り注文確定できる（既存機能への影響なし）

---

## 実装順序

```
T-1 → T-2 → T-3（例外基盤）
  → T-4（カート追加チェック）
  → T-5（カート取得時除外）
  → T-6（注文確定チェック）
    → T-7 → T-8, T-9（フロントエンド）
      → T-10, T-11（ドキュメント）
        → T-12（テスト）
```
