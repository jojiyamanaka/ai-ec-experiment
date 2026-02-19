# 在庫管理仕様書

## 概要

AI EC Experimentにおける在庫管理の振る舞いを定義する。

**関連ドキュメント**: [API仕様](../ui/api-spec.md), [データモデル](../data-model.md), [注文管理](./order.md)

---

## 1. 在庫状態の表示

| 在庫数 | 表示 | バッジ色 |
|--------|------|---------|
| 6以上 | 在庫あり | `bg-zinc-700` |
| 1〜5 | 残りわずか | `bg-zinc-500` |
| 0 | 売り切れ | `bg-zinc-400` |

---

## 2. カート追加時の在庫制御

| シナリオ | 条件 | 結果 |
|---------|------|------|
| 在庫0の商品 | `stock = 0` | ボタン無効化（`disabled`、ラベル「売り切れ」） |
| 新規追加 | `quantity ≤ stock` | カートに追加成功 |
| 新規追加 | `quantity > stock` | エラー: OUT_OF_STOCK |
| 既存アイテムへの追加 | `既存数量 + 追加数量 ≤ stock` | 数量を合算 |
| 既存アイテムへの追加 | `既存数量 + 追加数量 > stock` | エラー: OUT_OF_STOCK、数量変更なし |
| カート内数量変更 | `新数量 ≤ stock` | 数量更新 |
| カート内数量変更 | `新数量 > stock` | エラー: OUT_OF_STOCK、数量変更なし |

---

## 3. 注文時の在庫制御

**注文作成時**: 全カートアイテムの在庫をチェック
- 全商品で `stock ≥ quantity` → 注文作成、ステータス `PENDING`、カートクリア
- いずれかの商品で不足 → エラー: OUT_OF_STOCK、注文・カート変更なし

**注文作成後**: カートの全アイテムが削除される。

---

## 4. 在庫引当

在庫引当は **仮引当（Tentative）** と **本引当（Committed）** の2段階で管理する。

| 区分 | タイミング | 目的 | `products.stock` への影響 |
|------|-----------|------|--------------------------|
| 仮引当 | カートに商品追加時 | 他ユーザーとの競合防止 | 変更しない |
| 本引当 | 注文確定時 | 在庫を確定的に減少 | 減少させる |

**有効在庫** = `products.stock` − SUM(有効な仮引当の quantity) − SUM(本引当の quantity)

すべての在庫チェックは `products.stock` ではなく **有効在庫** に対して行う。

### 4-1. 仮引当の作成（カート追加時）

- `POST /api/inventory/reservations` — `X-Session-Id` 必須
- Request: `{ productId, quantity }`
- 有効在庫内 → `stock_reservations` にTENTATIVEレコード作成、`expires_at` = 現在+30分
- 有効在庫不足 → エラー: INSUFFICIENT_STOCK

### 4-2. 仮引当の更新（カート内数量変更時）

- 差分に対して有効在庫チェック
- 成功 → `quantity` 更新、`expires_at` リセット（現在+30分）

### 4-3. 仮引当の解除（カートから商品削除時）

- `DELETE /api/inventory/reservations/:id`
- 該当する仮引当レコードを削除、有効在庫が回復

### 4-4. 仮引当の自動失効

- **デフォルト有効期限**: 30分（カート操作のたびにリセット）
- **遅延評価**: 有効在庫計算クエリで `expires_at > CURRENT_TIMESTAMP` を条件にし、期限切れを自動除外
- **定期クリーンアップ**: `@Scheduled(fixedRate = 300_000)`（5分ごと）で期限切れの仮引当レコードを物理削除

```sql
-- 有効在庫計算
SELECT p.stock
  - COALESCE(SUM(CASE WHEN r.type = 'TENTATIVE' AND r.expires_at > CURRENT_TIMESTAMP THEN r.quantity ELSE 0 END), 0)
  - COALESCE(SUM(CASE WHEN r.type = 'COMMITTED' THEN r.quantity ELSE 0 END), 0)
  AS available_stock
FROM products p
LEFT JOIN stock_reservations r ON p.id = r.product_id
WHERE p.id = ?
```

### 4-5. 本引当の実行（注文確定時）

- `POST /api/inventory/reservations/commit` — `X-Session-Id` 必須
- Request: `{ cartId }`
- トランザクション内で:
  1. 全商品の有効在庫を再チェック
  2. `products.stock` を減少
  3. 仮引当 → 本引当（`TENTATIVE` → `COMMITTED`）に変換
  4. `order_id` を設定、`expires_at` を NULL に
  5. 注文レコード作成、カートクリア
- 有効在庫不足 → トランザクションロールバック、エラー: OUT_OF_STOCK

### 4-6. 本引当の解除（注文キャンセル時）

- `POST /api/order/:id/cancel`
- トランザクション内で:
  1. 注文ステータスを `CANCELLED` に変更
  2. `products.stock` を引当数量分だけ増加（在庫戻し）
  3. 本引当レコードを削除
- `SHIPPED` 以降はキャンセル不可 → エラー: ORDER_NOT_CANCELLABLE
- 既にキャンセル済み → エラー: ALREADY_CANCELLED

---

## 5. 在庫引当の状態遷移

```
カート追加 → TENTATIVE(仮引当) ──→ 期限切れ → 自動削除
                                  ──→ カート削除 → 削除
                │ 注文確定
                ▼
             COMMITTED(本引当) ──→ キャンセル → stock戻し → 削除
```

---

## 6. 在庫数の変更契機

| 契機 | `products.stock` | `stock_reservations` |
|------|-------------------|---------------------|
| 管理画面で手動更新 | 直接変更 | 変化なし |
| カートに追加 | 変化なし | 仮引当レコード作成 |
| カートから削除 | 変化なし | 仮引当レコード削除 |
| 仮引当タイムアウト | 変化なし | レコード削除（または無視） |
| 注文確定 | 減少 | 仮引当 → 本引当に変換 |
| 注文キャンセル | 増加（戻し） | 本引当レコード削除 |

管理画面での在庫変更はリアルタイムに反映される（次回API呼び出し時に新しい在庫数が反映）。

---

## 7. 排他制御・整合性

- 本引当（commit）時のみ `stock` を減少。減少前に `stock >= 引当数量` を必ずチェック
- チェックと減少はトランザクション内で排他的に実行
- PostgreSQL の `SELECT ... FOR UPDATE`（悲観ロック）を使用

---

## 8. エッジケース

| ケース | 動作 |
|--------|------|
| 同一商品の重複カート追加 | 既存カートアイテムに数量加算、仮引当の `quantity` を更新 |
| 非公開商品の在庫 | 商品一覧・詳細に非表示。カート内の仮引当は維持 |
| 仮引当期限切れ後のカート操作 | カートアイテムは残る。操作時に仮引当の再取得が必要。有効在庫不足ならエラー |
| 在庫数の上限 | 明示的な上限なし（データ型による制限のみ） |
| カート内商品の数量上限 | 有効在庫まで |

---

## 9. エラーコード一覧（在庫引当関連）

| コード | HTTP | 説明 |
|--------|------|------|
| INSUFFICIENT_STOCK | 409 | 有効在庫が不足しています |
| RESERVATION_NOT_FOUND | 404 | 引当が見つかりません |
| ORDER_NOT_CANCELLABLE | 400 | この注文はキャンセルできません |
| OUT_OF_STOCK | 409 | 在庫が不足している商品があります（注文確定時） |

---

## 実装クラス一覧

**バックエンド**: `InventoryService.java`, `InventoryController.java`, `StockReservation.java`, `StockReservationRepository.java`, `CartService.java`, `OrderService.java`
**フロントエンド**: `widgets/ProductCard/`（在庫状態表示）, `pages/customer/ItemDetailPage/`（カート追加制御）, `features/cart/model/CartContext.tsx`（カート状態管理）
