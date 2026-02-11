# 在庫管理仕様書

作成日: 2026-02-10
バージョン: 1.0

## 概要

本仕様書は、AI EC Experimentにおける在庫管理の振る舞いを定義する。
フロントエンドおよびバックエンドの実装を基に、Given/When/Then形式で明文化する。

---

## 1. 在庫状態の表示

### 1-1. 在庫が6個以上の場合

**Given**: 商品の `stock` が 6 以上
**When**: 商品一覧または商品詳細を表示する
**Then**:
- 在庫状態は「在庫あり」と表示される
- バッジの色は緑（`bg-green-500`）で表示される

**実装箇所**:
- フロントエンド: `frontend/src/components/ProductCard.tsx:20-24`
- フロントエンド: `frontend/src/pages/ItemDetailPage.tsx:18-23`

---

### 1-2. 在庫が1〜5個の場合

**Given**: 商品の `stock` が 1 以上 5 以下
**When**: 商品一覧または商品詳細を表示する
**Then**:
- 在庫状態は「残りわずか」と表示される
- バッジの色はオレンジ（`bg-orange-500`）で表示される

**実装箇所**:
- フロントエンド: `frontend/src/components/ProductCard.tsx:15-19`
- フロントエンド: `frontend/src/pages/ItemDetailPage.tsx:13-17`

---

### 1-3. 在庫が0個の場合

**Given**: 商品の `stock` が 0
**When**: 商品一覧または商品詳細を表示する
**Then**:
- 在庫状態は「売り切れ」と表示される
- バッジの色はグレー（`bg-gray-400`）で表示される

**実装箇所**:
- フロントエンド: `frontend/src/components/ProductCard.tsx:10-14`
- フロントエンド: `frontend/src/pages/ItemDetailPage.tsx:8-12`

---

## 2. カート追加時の在庫制御

### 2-1. 在庫が0の商品はカートに追加できない

**Given**: 商品の `stock` が 0
**When**: 商品詳細画面で「カートに追加」ボタンを表示する
**Then**:
- ボタンは無効化される（`disabled`）
- ボタンのラベルは「売り切れ」と表示される
- ボタンの背景色はグレー（`bg-gray-400`）になる
- クリックしても何も起きない

**実装箇所**:
- フロントエンド: `frontend/src/pages/ItemDetailPage.tsx:43,84-91`

---

### 2-2. カート追加時の在庫数チェック（新規追加）

**Given**:
- 商品の `stock` が N 個
- カートに同じ商品がまだ追加されていない

**When**: 商品詳細画面で「カートに追加」ボタンをクリックする（数量 Q 個）
**Then**:
- **Q ≤ N の場合**:
  - カートに商品が追加される
  - 成功メッセージが表示される（実装なし）
  - カート画面に遷移する
- **Q > N の場合**:
  - エラー「在庫が不足しています」が返される
  - カートには追加されない

**実装箇所**:
- バックエンド: `backend/src/main/java/com/example/aiec/service/CartService.java:54-56`

---

### 2-3. カート追加時の在庫数チェック（既存アイテムへの追加）

**Given**:
- 商品の `stock` が N 個
- カートに同じ商品が既に M 個追加されている

**When**: 同じ商品を「カートに追加」ボタンで追加する（追加数量 Q 個）
**Then**:
- 新しい数量 = M + Q
- **M + Q ≤ N の場合**:
  - カート内の数量が M から M + Q に更新される
  - カート画面に遷移する
- **M + Q > N の場合**:
  - エラー「在庫が不足しています」が返される
  - カート内の数量は M のまま変更されない

**実装箇所**:
- バックエンド: `backend/src/main/java/com/example/aiec/service/CartService.java:58-81`
- 特に: `CartService.java:62-72`（既存アイテムへの数量加算と在庫チェック）

---

### 2-4. カート内の数量変更時の在庫チェック

**Given**:
- 商品の `stock` が N 個
- カートに同じ商品が M 個追加されている

**When**: カート画面で数量を Q 個に変更する
**Then**:
- **Q ≤ N の場合**:
  - カート内の数量が Q に更新される
- **Q > N の場合**:
  - エラー「在庫が不足しています」が返される
  - カート内の数量は M のまま変更されない

**実装箇所**:
- バックエンド: `backend/src/main/java/com/example/aiec/service/CartService.java:103-106`

---

## 3. 注文時の在庫制御

### 3-1. 注文作成時の在庫チェック

**Given**:
- カートに複数の商品が入っている
- 各商品の `stock` と `quantity` の関係は様々

**When**: 注文確認画面で「注文を確定する」ボタンをクリックする
**Then**:
- すべてのカート内商品について在庫チェックを実行
- **すべての商品で stock ≥ quantity の場合**:
  - 注文が作成される
  - 注文番号（ORD-YYYYMMDD-XXX）が生成される
  - 注文ステータスは `PENDING` になる
  - カートがクリアされる
  - 注文完了画面に遷移する
- **いずれかの商品で stock < quantity の場合**:
  - エラー「在庫が不足している商品があります」が返される
  - 注文は作成されない
  - カートはクリアされない

**実装箇所**:
- バックエンド: `backend/src/main/java/com/example/aiec/service/OrderService.java:48-52`

**注意**:
- 在庫の引当処理（stock を減らす）は実装されていない
- 複数ユーザーが同時に同じ商品を注文した場合、在庫の競合が発生する可能性がある

---

### 3-2. 注文作成後のカートクリア

**Given**: 注文が正常に作成された
**When**: 注文作成処理が完了する
**Then**:
- カートの全アイテムが削除される
- カートは空になる

**実装箇所**:
- バックエンド: `backend/src/main/java/com/example/aiec/service/OrderService.java:72-73`

---

## 4. 在庫引当

在庫引当は **仮引当（Tentative）** と **本引当（Committed）** の2段階で管理する。

### 概念

| 区分 | タイミング | 目的 | `products.stock` への影響 |
|------|-----------|------|--------------------------|
| 仮引当 | カートに商品を追加した時 | 他ユーザーとの競合を防止 | 変更しない |
| 本引当 | 注文を確定した時 | 在庫を確定的に減少させる | 減少させる |

**有効在庫（available stock）** = `products.stock` − SUM(有効な仮引当の quantity) − SUM(本引当の quantity)

すべての在庫チェックは `products.stock` ではなく **有効在庫** に対して行う。

---

### データモデル: `stock_reservations` テーブル

```sql
CREATE TABLE stock_reservations (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    product_id  INTEGER      NOT NULL REFERENCES products(id),
    session_id  VARCHAR(255) NOT NULL,
    quantity    INTEGER      NOT NULL,
    type        VARCHAR(20)  NOT NULL,  -- 'TENTATIVE' | 'COMMITTED'
    order_id    INTEGER      REFERENCES orders(id),
    expires_at  TIMESTAMP,              -- 仮引当の有効期限（本引当では NULL）
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);
```

| カラム | 説明 |
|--------|------|
| `product_id` | 引当対象の商品 |
| `session_id` | 引当を行ったセッション |
| `quantity` | 引当数量 |
| `type` | `TENTATIVE`（仮引当）または `COMMITTED`（本引当） |
| `order_id` | 本引当時に紐づく注文ID（仮引当では NULL） |
| `expires_at` | 仮引当の有効期限（デフォルト: 作成から30分後） |

---

### 4-1. 仮引当の作成（カート追加時）

**Given**:
- 商品Aの `stock` が 10
- 他ユーザーによる仮引当が合計 3 個存在する
- 有効在庫 = 10 − 3 = 7

**When**:
- ユーザーが商品Aを 2 個カートに追加する
- `POST /api/inventory/reservations` が呼ばれる

**Then**:
- **2 ≤ 7（有効在庫）の場合**:
  - `stock_reservations` に仮引当レコードが作成される（type = `TENTATIVE`）
  - `expires_at` に現在時刻 + 30分が設定される
  - 有効在庫は 7 → 5 に減少する
  - カートへの追加が成功する
- **2 > 有効在庫 の場合**:
  - エラー `INSUFFICIENT_STOCK`「有効在庫が不足しています」が返される
  - 仮引当レコードは作成されない
  - カートへの追加は失敗する

---

### 4-2. 仮引当の更新（カート内数量変更時）

**Given**:
- 商品Aの仮引当が 2 個存在する（セッションXのもの）
- 有効在庫が 5

**When**:
- セッションXのユーザーがカート内の数量を 4 個に変更する

**Then**:
- 差分（+2）に対して有効在庫チェックを行う
- **差分 ≤ 有効在庫 の場合**:
  - 仮引当レコードの `quantity` を 4 に更新する
  - `expires_at` をリセット（現在時刻 + 30分）
- **差分 > 有効在庫 の場合**:
  - エラー `INSUFFICIENT_STOCK` が返される
  - 仮引当は元のまま（2 個）

---

### 4-3. 仮引当の解除（カートから商品削除時）

**Given**:
- 商品Aの仮引当が 2 個存在する（セッションXのもの）

**When**:
- セッションXのユーザーがカートから商品Aを削除する
- `DELETE /api/inventory/reservations/:id` が呼ばれる

**Then**:
- 該当する仮引当レコードが削除される
- 有効在庫が 2 個分回復する

---

### 4-4. 仮引当の自動失効（タイムアウト）

**Given**:
- 商品Aの仮引当が 2 個存在する（セッションXのもの）
- `expires_at` が現在時刻を過ぎている

**When**:
- 有効在庫を計算する（任意のAPIリクエスト時）

**Then**:
- 期限切れの仮引当は有効在庫の計算に含めない
- 期限切れレコードは次回アクセス時またはバッチ処理で削除される

**補足**:
- 仮引当のデフォルト有効期限: **30分**
- カート操作を行うたびに `expires_at` はリセットされる

**失効の実装方式（遅延評価 + 定期クリーンアップ）**:

1. **遅延評価（メイン）**: 有効在庫の計算クエリで `expires_at > CURRENT_TIMESTAMP` を条件にし、期限切れレコードを自動的に除外する。削除処理なしで即座に無効になる。
```sql
SELECT p.stock
  - COALESCE(SUM(CASE WHEN r.type = 'TENTATIVE' AND r.expires_at > CURRENT_TIMESTAMP THEN r.quantity ELSE 0 END), 0)
  - COALESCE(SUM(CASE WHEN r.type = 'COMMITTED' THEN r.quantity ELSE 0 END), 0)
  AS available_stock
FROM products p
LEFT JOIN stock_reservations r ON p.id = r.product_id
WHERE p.id = ?
```

2. **定期クリーンアップ（ゴミ掃除）**: `@Scheduled(fixedRate = 300_000)`（5分ごと）で期限切れの仮引当レコードを物理削除し、テーブルの肥大化を防ぐ。
```java
@Scheduled(fixedRate = 300_000)
public void cleanupExpiredReservations() {
    reservationRepository.deleteByTypeAndExpiresAtBefore(
        ReservationType.TENTATIVE, LocalDateTime.now());
}
```

---

### 4-5. 本引当の実行（注文確定時）

**Given**:
- カートに商品A × 2個、商品B × 1個が入っている
- 商品A、Bそれぞれに仮引当が存在する

**When**:
- ユーザーが注文を確定する
- `POST /api/inventory/reservations/commit` が呼ばれる

**Then**:
- トランザクション内で以下を実行:
  1. 全商品の有効在庫を再チェック（最終確認）
  2. `products.stock` を減少させる（商品A: stock -= 2, 商品B: stock -= 1）
  3. 仮引当レコードの `type` を `TENTATIVE` → `COMMITTED` に変更
  4. `order_id` に注文IDを設定
  5. `expires_at` を NULL に設定（本引当は期限なし）
  6. 注文レコードを作成
  7. カートをクリア
- **いずれかの商品で有効在庫不足の場合**:
  - トランザクションをロールバック
  - エラー `OUT_OF_STOCK` が返される
  - `products.stock` は変更されない
  - 仮引当は維持される
  - カートはクリアされない

---

### 4-6. 本引当の解除（注文キャンセル時） ✅ 実装済み

**API**: `POST /api/order/:id/cancel`

**Given**:
- 注文が存在する（ステータス: `PENDING` または `CONFIRMED`）
- 本引当レコードが存在する（type = `COMMITTED`）

**When**:
- 注文がキャンセルされる
- `POST /api/order/:id/cancel` が呼ばれる

**Then**:
- トランザクション内で以下を実行:
  1. 注文ステータスを `CANCELLED` に変更
  2. `products.stock` を引当数量分だけ増加させる（在庫を戻す）
  3. 本引当レコードを削除する
- **`SHIPPED` 以降のステータスではキャンセル不可**:
  - エラー `ORDER_NOT_CANCELLABLE`「この注文はキャンセルできません」が返される
- **既にキャンセル済みの注文は再キャンセル不可**:
  - エラー `ALREADY_CANCELLED`「この注文は既にキャンセルされています」が返される

**実装**:
- `InventoryService.releaseCommittedReservations()` が在庫戻し処理を実行
- `OrderService.cancelOrder()` が注文キャンセルエンドポイントを提供

---

## 5. 在庫引当 API

### 5-1. 仮引当作成

**エンドポイント**:
```
POST /api/inventory/reservations
```

**ヘッダー**:
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**リクエストボディ**:
```json
{
  "productId": 1,
  "quantity": 2
}
```

**成功レスポンス（201 Created）**:
```json
{
  "success": true,
  "data": {
    "reservationId": 1,
    "productId": 1,
    "quantity": 2,
    "type": "TENTATIVE",
    "expiresAt": "2026-02-10T16:00:00",
    "availableStock": 5
  }
}
```

**エラーレスポンス（409 Conflict）**:
```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_STOCK",
    "message": "有効在庫が不足しています",
    "details": {
      "productId": 1,
      "requestedQuantity": 2,
      "availableStock": 1
    }
  }
}
```

---

### 5-2. 仮引当解除

**エンドポイント**:
```
DELETE /api/inventory/reservations/:id
```

**ヘッダー**:
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**成功レスポンス（200 OK）**:
```json
{
  "success": true,
  "data": {
    "releasedQuantity": 2,
    "availableStock": 7
  }
}
```

**エラーレスポンス（404 Not Found）**:
```json
{
  "success": false,
  "error": {
    "code": "RESERVATION_NOT_FOUND",
    "message": "引当が見つかりません"
  }
}
```

---

### 5-3. 本引当（注文確定時）

**エンドポイント**:
```
POST /api/inventory/reservations/commit
```

**ヘッダー**:
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**リクエストボディ**:
```json
{
  "cartId": "session-cart-id"
}
```

**成功レスポンス（200 OK）**:
```json
{
  "success": true,
  "data": {
    "committedReservations": [
      {
        "reservationId": 1,
        "productId": 1,
        "quantity": 2
      },
      {
        "reservationId": 2,
        "productId": 3,
        "quantity": 1
      }
    ],
    "orderId": 5
  }
}
```

**エラーレスポンス（409 Conflict）**:
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
        "requestedQuantity": 2,
        "availableStock": 1
      }
    ]
  }
}
```

---

### 5-4. 本引当解除（注文キャンセル時）

**エンドポイント**:
```
POST /api/inventory/reservations/release
```

**ヘッダー**:
| ヘッダー | 値 | 必須 | 説明 |
|---------|-----|------|------|
| X-Session-Id | string | ○ | セッションID |

**リクエストボディ**:
```json
{
  "orderId": 5
}
```

**成功レスポンス（200 OK）**:
```json
{
  "success": true,
  "data": {
    "releasedReservations": [
      {
        "productId": 1,
        "quantity": 2,
        "restoredStock": 10
      }
    ],
    "orderStatus": "CANCELLED"
  }
}
```

**エラーレスポンス（400 Bad Request）**:
```json
{
  "success": false,
  "error": {
    "code": "ORDER_NOT_CANCELLABLE",
    "message": "この注文はキャンセルできません"
  }
}
```

---

### 5-5. 有効在庫確認

**エンドポイント**:
```
GET /api/inventory/availability/:productId
```

**成功レスポンス（200 OK）**:
```json
{
  "success": true,
  "data": {
    "productId": 1,
    "physicalStock": 10,
    "tentativeReserved": 3,
    "committedReserved": 2,
    "availableStock": 5
  }
}
```

---

## 6. 在庫引当の状態遷移

```
                  カート追加
                     │
                     ▼
              ┌──────────────┐
              │   TENTATIVE  │──── 期限切れ ────→ 自動削除
              │   (仮引当)    │──── カート削除 ──→ 削除
              └──────┬───────┘
                     │ 注文確定
                     ▼
              ┌──────────────┐
              │  COMMITTED   │──── キャンセル ──→ stock戻し → 削除
              │   (本引当)    │
              └──────────────┘
```

---

## 7. 有効在庫の計算ルール

### 7-1. 有効在庫の算出

**Given**: 商品Aの状態が以下の通り
- `products.stock` = 10
- 仮引当（期限内）: セッションX = 2個, セッションY = 3個
- 仮引当（期限切れ）: セッションZ = 1個
- 本引当: 注文#1 = 2個

**When**: 有効在庫を計算する

**Then**:
- 有効在庫 = 10 − (2 + 3) − 2 = 3
- 期限切れの仮引当（セッションZ = 1個）は計算に含めない

---

### 7-2. 有効在庫がゼロの場合

**Given**: 商品Bの有効在庫が 0

**When**: 新しいユーザーが商品Bをカートに追加しようとする

**Then**:
- エラー `INSUFFICIENT_STOCK` が返される
- `products.stock` が 0 でなくても、仮引当・本引当で埋まっていれば追加できない

---

## 8. 既存APIとの統合

### 8-1. カート追加 (`POST /api/order/cart/items`) への影響

**変更前**: `products.stock` のみチェック
**変更後**: 仮引当APIを内部的に呼び出し、有効在庫をチェック

**処理の流れ**:
1. `POST /api/order/cart/items` を受け取る
2. 内部で `POST /api/inventory/reservations` を呼ぶ
3. 仮引当が成功 → カートに商品を追加
4. 仮引当が失敗 → エラーを返す

---

### 8-2. カート数量変更 (`PUT /api/order/cart/items/:id`) への影響

**変更前**: `products.stock` のみチェック
**変更後**: 仮引当の数量を更新し、有効在庫をチェック

---

### 8-3. カート削除 (`DELETE /api/order/cart/items/:id`) への影響

**変更前**: カートアイテムのみ削除
**変更後**: 対応する仮引当も解除

---

### 8-4. 注文作成 (`POST /api/order`) への影響

**変更前**: `products.stock` をチェックするのみ（stock は減少しない）
**変更後**: 内部で本引当APIを呼び出し、`products.stock` を減少させる

**処理の流れ**:
1. `POST /api/order` を受け取る
2. 内部で `POST /api/inventory/reservations/commit` を呼ぶ
3. 本引当が成功 → 注文作成 → カートクリア
4. 本引当が失敗 → エラーを返す

---

## 9. 在庫のマイナス値防止

### 9-1. `products.stock` はマイナスにならない

**Given**: 任意の操作
**When**: `products.stock` を変更する
**Then**:
- 本引当（commit）時のみ `stock` を減少させる
- 減少前に `stock >= 引当数量` を必ずチェック
- チェックと減少はトランザクション内で排他的に実行する
- 排他制御: SQLite の SERIALIZABLE 分離レベル（デフォルト）を利用。SQLite はデータベースレベルのロックにより同時書き込みを直列化するため、行ロック（`SELECT ... FOR UPDATE`）なしで排他制御が成立する
- 本番環境で PostgreSQL 等に移行する場合は、`SELECT ... FOR UPDATE`（悲観ロック）への変更を検討

---

## 10. 注文キャンセル時の在庫戻し

### 10-1. キャンセル可能な注文ステータス

**Given**: 注文ステータスが `PENDING` または `CONFIRMED`
**When**: キャンセルリクエストを送信する
**Then**:
- 注文ステータスが `CANCELLED` に変更される
- 本引当レコードが削除される
- `products.stock` が引当数量分だけ増加する

### 10-2. キャンセル不可な注文ステータス

**Given**: 注文ステータスが `SHIPPED` または `DELIVERED`
**When**: キャンセルリクエストを送信する
**Then**:
- エラー `ORDER_NOT_CANCELLABLE` が返される
- 注文ステータスは変更されない
- `products.stock` も変更されない

---

## 11. 在庫状態の更新タイミング

### 11-1. 在庫数の変更契機

| 契機 | `products.stock` の変化 | `stock_reservations` の変化 |
|------|------------------------|----------------------------|
| 管理画面で手動更新 | 直接変更 | 変化なし |
| カートに追加 | 変化なし | 仮引当レコード作成 |
| カートから削除 | 変化なし | 仮引当レコード削除 |
| 仮引当タイムアウト | 変化なし | レコード削除（または無視） |
| 注文確定 | 減少 | 仮引当 → 本引当に変換 |
| 注文キャンセル | 増加（戻し） | 本引当レコード削除 |

---

### 11-2. 在庫数の変更はリアルタイムで反映される

**Given**: 管理画面で商品の在庫数を変更して保存する
**When**: 変更が保存される
**Then**:
- データベースの `stock` が即座に更新される
- 有効在庫も即座に再計算される
- 商品一覧・詳細画面で次回表示時に新しい在庫数が反映される

---

## 12. エッジケースと制約

### 12-1. 在庫数の上限

**制約**: なし
**説明**: 在庫数に明示的な上限は設定されていない（データ型による制限のみ）

---

### 12-2. カート内商品の数量上限

**制約**: 有効在庫まで
**説明**: 仮引当の仕組みにより、カート内の数量は有効在庫を超えられない

---

### 12-3. 同一商品の重複カート追加

**動作**: 既存のカートアイテムに数量が加算される
**引当への影響**: 既存の仮引当レコードの `quantity` を更新する

---

### 12-4. 非公開商品の在庫

**Given**: 商品が非公開（`isPublished = false`）
**When**: その商品の在庫を確認する
**Then**:
- 商品一覧には表示されない
- 商品詳細APIでもアクセス不可（H-1修正済み）
- カートに既に入っている場合は、仮引当は維持される

---

### 12-5. 仮引当の有効期限中にカートを放置した場合

**Given**:
- ユーザーが商品Aを 2個カートに追加した（仮引当済み）
- 30分以上操作しなかった

**When**: 仮引当の有効期限が切れる

**Then**:
- 仮引当は無効になる（有効在庫の計算から除外される）
- カート内の商品は残る（カートアイテムは削除しない）
- ユーザーがカート操作や注文を行う際に、仮引当の再取得が必要
- 再取得時に有効在庫が不足していれば、エラーが返される

---

## 13. エラーコード一覧（在庫引当関連）

| コード | HTTPステータス | 説明 |
|--------|---------------|------|
| INSUFFICIENT_STOCK | 409 | 有効在庫が不足しています |
| RESERVATION_NOT_FOUND | 404 | 引当が見つかりません |
| ORDER_NOT_CANCELLABLE | 400 | この注文はキャンセルできません |
| OUT_OF_STOCK | 409 | 在庫が不足している商品があります（注文確定時） |

---

## 14. 参考情報

### 関連仕様書
- `docs/SPEC.md`: 機能仕様書（在庫状態のルール: 255-280行目）
- `docs/ui/api-spec.md`: API仕様書（在庫エラーレスポンス: 437-453行目）
- `docs/specs/order.md`: 注文管理仕様書
- `docs/ui/admin-ui.md`: 管理画面UI仕様書
- `docs/spec-implementation-gaps.md`: H-2（在庫引当未実装の課題）

### 関連実装
- **フロントエンド**:
  - `frontend/src/components/ProductCard.tsx`: 在庫状態表示ロジック
  - `frontend/src/pages/ItemDetailPage.tsx`: カート追加制御
  - `frontend/src/contexts/CartContext.tsx`: カート状態管理

- **バックエンド**:
  - `backend/src/main/java/com/example/aiec/service/CartService.java`: カート在庫チェック
  - `backend/src/main/java/com/example/aiec/service/OrderService.java`: 注文時在庫チェック
  - `backend/src/main/java/com/example/aiec/entity/Product.java`: 商品エンティティ

### 新規追加予定
- **テーブル**: `stock_reservations`
- **エンティティ**: `StockReservation.java`
- **リポジトリ**: `StockReservationRepository.java`
- **サービス**: `InventoryService.java`
- **コントローラ**: `InventoryController.java`

---

## 変更履歴

| バージョン | 日付 | 変更内容 | 作成者 |
|-----------|------|---------|--------|
| 1.0 | 2026-02-10 | 初版作成 | Claude Sonnet 4.5 |
| 2.0 | 2026-02-10 | 在庫引当機能（仮引当・本引当）の仕様追加 | Claude Opus 4.6 |
