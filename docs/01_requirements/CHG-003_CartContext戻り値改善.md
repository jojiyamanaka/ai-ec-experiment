# REQ-003: CartContext の戻り値改善

作成日: 2026-02-12
ステータス: 起票済み
優先度: 中
起源: REQ-002 実装レビューでの指摘

---

## 1. 背景

現在の `CartContext` のカート操作メソッド（`addToCart`, `removeFromCart`, `updateQuantity`）は、エラー時に `throw new Error(message)` でメッセージ文字列のみをスローしている。そのため、呼び出し元でエラーコード（`ITEM_NOT_AVAILABLE`, `INSUFFICIENT_STOCK` 等）による分岐ができない。

REQ-002 の実装で `ItemDetailPage` がこの制約を回避するために `api.addToCart()` を直接呼び出す実装になったが、他のページ（`CartPage` 等）は引き続き Context 経由のため、パターンが不統一になっている。

## 2. 要件

### 2-1. カート操作メソッドがエラー詳細を返すようにする

`addToCart`, `removeFromCart`, `updateQuantity` の戻り値を `Promise<ApiResponse<Cart>>` に変更し、呼び出し元でエラーコード・詳細にアクセスできるようにする。

### 2-2. 全ページで Context 経由のカート操作に統一する

ページコンポーネントから `api.*` のカート操作を直接呼び出さず、`CartContext` 経由に統一する。

### 2-3. 既存ページのエラーハンドリングを改善する

`CartPage` 等で `alert()` を使っているエラー表示を、UI上のエラーメッセージ表示に置き換える。

---

## 3. 受け入れ条件

- [ ] `CartContext` のカート操作メソッドが `ApiResponse` を返す
- [ ] 呼び出し元でエラーコード（`error.code`）による条件分岐ができる
- [ ] 全ページが Context 経由でカート操作を行っている（`api.*` 直接呼び出しがない）
- [ ] `alert()` によるエラー表示が UI 表示に置き換えられている

---

## 4. 関連資料

- REQ-002 実装レビュー（指摘1: ItemDetailPage の API 直接呼び出し）
- `frontend/src/contexts/CartContext.tsx`
- `frontend/src/pages/ItemDetailPage.tsx`
- `frontend/src/pages/CartPage.tsx`
