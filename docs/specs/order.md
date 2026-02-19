# 注文管理仕様書

## 概要

AI EC Experimentにおける注文管理の振る舞いを定義する。

**関連ドキュメント**: [API仕様](../ui/api-spec.md), [データモデル](../data-model.md), [在庫管理](./inventory.md)

---

## 1. 注文ステータス

| ステータス | 英語表記 | 説明 |
|-----------|---------|------|
| 作成済み | PENDING | 注文が作成された初期状態 |
| 確認済み | CONFIRMED | 注文内容が確認され、在庫本引当済み |
| 出荷準備中 | PREPARING_SHIPMENT | 出荷指示待ちの状態（JobRunr による自動処理） |
| 発送済み | SHIPPED | 商品が発送された状態 |
| 配達完了 | DELIVERED | 商品が配達完了した状態 |
| キャンセル | CANCELLED | 注文がキャンセルされた状態 |

---

## 2. 状態遷移

| 遷移 | トリガー | バリデーション | 副作用 |
|------|---------|---------------|--------|
| PENDING → CONFIRMED | `POST /api/order/:id/confirm`（管理者） | PENDING以外は不可 | 在庫本引当、イベント発行 |
| CONFIRMED → PREPARING_SHIPMENT | CreateShipmentJob（自動） | CONFIRMED以外は不可 | Shipment生成 |
| PREPARING_SHIPMENT → SHIPPED | `POST /api/order/:id/mark-shipped`（管理者） | PREPARING_SHIPMENT以外は不可 | updatedAt更新 |
| SHIPPED → DELIVERED | `POST /api/order/:id/deliver`（管理者） | SHIPPED以外は不可 | updatedAt更新 |
| PENDING/CONFIRMED → CANCELLED | `POST /api/order/:id/cancel` | SHIPPED/DELIVERED不可 | 本引当削除、stock戻し、updatedAt更新 |

**エラー**:
- 不正な遷移 → `INVALID_STATUS_TRANSITION`
- キャンセル不可 → `ORDER_NOT_CANCELLABLE`
- 再キャンセル → `ALREADY_CANCELLED`

---

## 3. 注文作成の条件

| 条件 | 結果 |
|------|------|
| カートが空 | エラー: CART_EMPTY |
| いずれかの商品で在庫不足 | エラー: OUT_OF_STOCK、注文・カート変更なし |
| X-Session-Id と cartId の不一致 | エラー: INVALID_REQUEST |
| 全条件OK | 注文作成（PENDING）、カートクリア |

### 会員とゲストの区別

| ユーザー種別 | `Order.userId` | `Order.sessionId` | 識別方法 |
|-------------|---------------|-------------------|---------|
| ゲスト | null | セッションID | sessionIdで識別 |
| 会員 | 会員ID | セッションID（互換性） | userIdで識別 |

### 注文の所有権確認

- **会員の注文**: `userId` が一致する場合のみアクセス可能
- **ゲストの注文**: `userId = null` かつ `sessionId` が一致する場合のみ
- **管理者**: すべての注文にアクセス可能

### 注文履歴取得（会員専用）

- `GET /api/order/history` — 認証必須
- 自分の注文のみ（`userId` でフィルタ）、作成日時降順、全ステータス含む

---

## 4. 注文番号の採番ルール

- 形式: `ORD-xxxxxxxxxx`（0埋め10桁の連番）
- 例: `ORD-0000000001`, `ORD-0000000002`
- データベースで一意制約（`unique = true`）
- 連番はグローバルに単調増加（PostgreSQLシーケンス使用）

---

## 5. 注文作成後の挙動

**成功時**:
1. バックエンドで注文保存 → カートクリア
2. フロントエンドでローカルカート状態クリア
3. 注文完了画面（`/order/complete`）に遷移（React Router の `state` で情報受け渡し）

**失敗時**: カート・注文とも変更なし。ユーザーはカートに戻って修正可能。

**注文完了画面の直接URLアクセス**: `state` が null のためエラーメッセージ「注文情報が見つかりません」を表示。

---

## 6. 各状態でできる操作

| 状態 | 取得 | キャンセル | 確認 | 発送 | 配達完了 |
|------|------|----------|------|------|---------|
| PENDING | ○ | ○ | ○(管理者) | × | × |
| CONFIRMED | ○ | ○ | × | ○(管理者) | × |
| SHIPPED | ○ | × | × | × | ○(管理者) |
| DELIVERED | ○ | × | × | × | × |
| CANCELLED | ○ | × | × | × | × |

---

## 7. エッジケースと制約

| ケース | 動作 |
|--------|------|
| 同時注文の競合 | トランザクション制御あり。在庫引当で排他制御 |
| 注文作成後の在庫変動 | 注文データは変更されない（OrderItem にスナップショット保存） |
| 配送料・手数料 | 常に ¥0（Phase 1）。合計 = 商品小計 |

---

## 8. 未実装機能（Phase 2以降）

- 決済処理（決済サービス連携、キャンセル時の返金）
- 配送情報管理（配送先住所、追跡番号）
- 通知機能（注文確認メール、発送通知）
- 注文内容の変更

---

## 実装クラス一覧

**バックエンド**: `OrderService.java`, `OrderController.java`, `Order.java`, `OrderItem.java`, `InventoryService.java`
**フロントエンド**: `pages/customer/OrderConfirmPage/`, `pages/customer/OrderCompletePage/`, `features/cart/model/CartContext.tsx`
