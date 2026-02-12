# 注文管理仕様書

作成日: 2026-02-10
バージョン: 1.0

## 概要

本仕様書は、AI EC Experimentにおける注文管理の振る舞いを定義する。
フロントエンドおよびバックエンドの実装を基に、Given/When/Then形式で明文化する。

---

## 1. 注文の状態定義

### 1-1. 注文ステータスの種類

注文は以下の5つのステータスを持つ：

| ステータス | 英語表記 | 説明 | 実装状況 |
|-----------|---------|------|---------|
| 作成済み | PENDING | 注文が作成された初期状態 | ✅ 実装済み |
| 確認済み | CONFIRMED | 注文内容が確認され、処理が開始された状態 | ❌ 未実装 |
| 発送済み | SHIPPED | 商品が発送された状態 | ❌ 未実装 |
| 配達完了 | DELIVERED | 商品が配達完了した状態 | ❌ 未実装 |
| キャンセル | CANCELLED | 注文がキャンセルされた状態 | ❌ 未実装 |

**実装箇所**: `backend/src/main/java/com/example/aiec/entity/Order.java:70-76`

---

### 1-2. 注文作成時の初期状態

**Given**: 注文を作成する
**When**: 注文作成処理が成功する
**Then**:
- 注文のステータスは `PENDING` になる
- 注文番号（ORD-YYYYMMDD-XXX形式）が生成される
- 作成日時（createdAt）と更新日時（updatedAt）が自動的に設定される

**実装箇所**:
- バックエンド: `OrderService.java:59`（ステータス設定）
- バックエンド: `Order.java:48-52`（タイムスタンプ自動設定）

---

## 2. 注文作成の条件

### 2-1. カートが空の場合は注文作成できない

**Given**: カートに商品が1つも入っていない
**When**: 注文作成APIを呼び出す
**Then**:
- エラーコード「CART_EMPTY」が返される
- エラーメッセージ「カートが空です」が返される
- 注文は作成されない
- カートはクリアされない

**実装箇所**:
- バックエンド: `OrderService.java:42-45`
- フロントエンド: `OrderConfirmPage.tsx:12-30`（UIでの事前チェック）

---

### 2-2. 在庫が不足している場合は注文作成できない

**Given**:
- カートに複数の商品が入っている
- いずれかの商品で stock < quantity

**When**: 注文作成APIを呼び出す
**Then**:
- エラーコード「OUT_OF_STOCK」が返される
- エラーメッセージ「在庫が不足している商品があります」が返される
- 注文は作成されない
- カートはクリアされない
- どの商品が不足しているかの詳細情報は返されない（仕様上は返すべき）

**実装箇所**:
- バックエンド: `OrderService.java:47-52`

**注意**:
- API仕様書では `details` 配列で不足商品の詳細を返すべきとされているが、実装されていない
- 参照: `docs/gap-analysis.md` の「2-6」

---

### 2-3. セッションIDとカートIDが一致しない場合は注文作成できない

**Given**:
- リクエストヘッダーの `X-Session-Id` が "session-A"
- リクエストボディの `cartId` が "session-B"

**When**: 注文作成APIを呼び出す
**Then**:
- エラーコード「INVALID_REQUEST」が返される
- エラーメッセージ「無効なリクエストです」が返される
- 注文は作成されない

**実装箇所**:
- バックエンド: `OrderService.java:34-37`

---

## 3. 注文番号の採番ルール

### 3-1. 注文番号の形式

**Given**: 注文を作成する
**When**: 注文番号が生成される
**Then**:
- 形式: `ORD-YYYYMMDD-XXX`
- YYYYMMDD: 作成日（例: 20260210）
- XXX: 同日の注文連番（001から始まる3桁、上限999件/日）
- 例: `ORD-20260210-001`, `ORD-20260210-002`
- 1日あたりの注文上限は999件。超過時は4桁以上になるが、Phase 1 では対策不要（プロトタイプ規模のため）

**実装箇所**:
- バックエンド: `OrderService.java:97-108`

---

### 3-2. 注文番号の連番ロジック

**Given**:
- 本日（2026-02-10）の注文が既に3件ある
- 最新の注文番号は `ORD-20260210-003`

**When**: 新しい注文を作成する
**Then**:
- 新しい注文番号は `ORD-20260210-004` になる
- 連番は同日内で継続する
- 翌日になると連番は `001` からリセットされる

**実装箇所**:
- バックエンド: `OrderService.java:101-107`

**実装の詳細**:
```java
// 今日の注文数を取得
long todayOrderCount = orderRepository.findAll().stream()
    .filter(o -> o.getOrderNumber().startsWith(prefix))
    .count();
int sequence = (int) (todayOrderCount + 1);
return prefix + String.format("%03d", sequence);
```

---

### 3-3. 注文番号の一意性保証

**Given**: 注文番号が生成される
**When**: データベースに保存する
**Then**:
- 注文番号は一意である（ユニーク制約）
- 同じ注文番号は存在しない

**実装箇所**:
- バックエンド: `Order.java:26-27`（`unique = true` 制約）

---

## 4. 注文作成後の挙動

### 4-1. 注文作成成功時のカートクリア

**Given**: 注文作成が成功する
**When**: 注文作成処理が完了する
**Then**:
- バックエンド側でカートの全アイテムが削除される
- フロントエンド側でもカートのローカル状態がクリアされる
- カートは空になる

**実装箇所**:
- バックエンド: `OrderService.java:72-73`
- フロントエンド: `OrderConfirmPage.tsx:43`

**処理の流れ**:
1. バックエンドで注文を保存
2. バックエンドでカートをクリア（`cartService.clearCart(sessionId)`）
3. フロントエンドで成功レスポンスを受け取る
4. フロントエンドでローカル状態をクリア（`clearCart()`）

---

### 4-2. 注文作成失敗時のカート保持

**Given**: 注文作成が失敗する（在庫不足など）
**When**: エラーレスポンスが返される
**Then**:
- カートはクリアされない
- カート内の商品はそのまま残る
- ユーザーはカートに戻って修正できる

**実装箇所**:
- バックエンド: `OrderService.java:47-52`（エラー時は早期リターン）
- フロントエンド: `OrderConfirmPage.tsx:57-66`（エラーハンドリング）

---

### 4-3. 注文完了画面への遷移

**Given**: 注文作成が成功する
**When**: フロントエンドが成功レスポンスを受け取る
**Then**:
- 注文完了画面（`/order/complete`）に遷移する
- 注文情報を React Router の `state` で受け渡す
- 受け渡される情報:
  - `orderNumber`: 注文番号
  - `items`: 注文商品リスト
  - `totalPrice`: 合計金額
  - `orderId`: 注文ID

**実装箇所**:
- フロントエンド: `OrderConfirmPage.tsx:46-53`

---

## 5. 注文詳細の取得

### 5-1. 注文詳細取得時のセッションID検証

**Given**:
- 注文が存在する（注文ID: 123, セッションID: "session-A"）
- リクエストヘッダーの `X-Session-Id` が "session-A"

**When**: 注文詳細取得API（`GET /api/order/123`）を呼び出す
**Then**:
- 注文の詳細情報が返される
- 注文番号、商品リスト、合計金額、ステータスなどが含まれる

**実装箇所**:
- バックエンド: `OrderService.java:82-92`

---

### 5-2. 他人の注文は取得できない

**Given**:
- 注文が存在する（注文ID: 123, セッションID: "session-A"）
- リクエストヘッダーの `X-Session-Id` が "session-B"（別のセッション）

**When**: 注文詳細取得API（`GET /api/order/123`）を呼び出す
**Then**:
- エラーコード「ORDER_NOT_FOUND」が返される
- エラーメッセージ「注文が見つかりません」が返される
- 注文の詳細情報は返されない

**実装箇所**:
- バックエンド: `OrderService.java:86-89`

**セキュリティ上の理由**:
- 他人の注文情報を取得されないようにする
- セッションIDで注文の所有者を検証

---

## 6. 注文完了画面の挙動

### 6-1. 正常な注文完了画面の表示

**Given**: 注文確認画面から正しい手順で遷移してきた
**When**: 注文完了画面（`/order/complete`）にアクセスする
**Then**:
- 注文完了メッセージが表示される
- 注文番号が強調表示される
- 注文商品の一覧が表示される
- 合計金額が表示される
- お知らせメッセージが表示される
- 「TOPに戻る」「買い物を続ける」ボタンが表示される

**実装箇所**:
- フロントエンド: `OrderCompletePage.tsx:38-149`

---

### 6-2. 直接URLアクセス時のエラー表示

**Given**: ブラウザのアドレスバーに直接 `/order/complete` を入力する
**When**: 注文完了画面にアクセスする
**Then**:
- エラーメッセージ「注文情報が見つかりません」が表示される
- 案内メッセージ「正しい手順で注文を完了してください」が表示される
- 「TOPに戻る」ボタンのみ表示される
- 注文情報は表示されない

**実装箇所**:
- フロントエンド: `OrderCompletePage.tsx:14-34`

**理由**:
- React Router の `state` は遷移時のみ渡される
- 直接URLアクセスやリロードでは `state` が null になる

---

## 7. 注文の状態遷移 ✅ 実装済み

### 7-1. PENDING → CONFIRMED への遷移 ✅

**API**: `POST /api/order/:id/confirm`

**Given**: 注文のステータスが `PENDING`
**When**: 管理者が注文内容を確認する
**Then**:
- ✅ 注文ステータスが `CONFIRMED` に変更される
- ✅ 更新日時（`updatedAt`）が更新される
- ✅ 変更後の注文データを返す
- ⚠️ 決済処理は未実装（Phase 2以降）

**バリデーション**:
- PENDING 以外の状態からは遷移不可
- エラー `INVALID_STATUS_TRANSITION`「この注文は確認できません（現在のステータス: XXX）」

**実装箇所**:
- `OrderService.confirmOrder()` - backend/src/main/java/com/example/aiec/service/OrderService.java
- `OrderController.confirmOrder()` - backend/src/main/java/com/example/aiec/controller/OrderController.java

---

### 7-2. CONFIRMED → SHIPPED への遷移 ✅

**API**: `POST /api/order/:id/ship`

**Given**: 注文のステータスが `CONFIRMED`
**When**: 管理者が商品を発送する
**Then**:
- ✅ 注文ステータスが `SHIPPED` に変更される
- ✅ 更新日時（`updatedAt`）が更新される
- ✅ 変更後の注文データを返す
- ⚠️ 追跡番号発行・発送通知メールは未実装（Phase 2以降）

**バリデーション**:
- CONFIRMED 以外の状態からは遷移不可
- エラー `INVALID_STATUS_TRANSITION`「この注文は発送できません（現在のステータス: XXX）」

**実装箇所**:
- `OrderService.shipOrder()` - backend/src/main/java/com/example/aiec/service/OrderService.java
- `OrderController.shipOrder()` - backend/src/main/java/com/example/aiec/controller/OrderController.java

---

### 7-3. SHIPPED → DELIVERED への遷移 ✅

**API**: `POST /api/order/:id/deliver`

**Given**: 注文のステータスが `SHIPPED`
**When**: 配送業者が商品を配達完了する
**Then**:
- ✅ 注文ステータスが `DELIVERED` に変更される
- ✅ 更新日時（`updatedAt`）が更新される
- ✅ 変更後の注文データを返す
- ⚠️ 配達完了通知は未実装（Phase 2以降）

**バリデーション**:
- SHIPPED 以外の状態からは遷移不可
- エラー `INVALID_STATUS_TRANSITION`「この注文は配達完了にできません（現在のステータス: XXX）」

**実装箇所**:
- `OrderService.deliverOrder()` - backend/src/main/java/com/example/aiec/service/OrderService.java
- `OrderController.deliverOrder()` - backend/src/main/java/com/example/aiec/controller/OrderController.java

---

### 7-4. PENDING/CONFIRMED → CANCELLED への遷移 ✅

**API**: `POST /api/order/:id/cancel`

**Given**: 注文のステータスが `PENDING` または `CONFIRMED`
**When**: 顧客または管理者が注文をキャンセルする
**Then**:
- ✅ 注文ステータスが `CANCELLED` に変更される
- ✅ 本引当レコード（`stock_reservations`）を削除
- ✅ `products.stock` を引当数量分だけ増加（在庫を戻す）
- ✅ 更新日時（`updatedAt`）が更新される
- ⚠️ 決済キャンセル・通知メールは未実装（Phase 2以降）

**バリデーション**:
- SHIPPED/DELIVERED 状態からはキャンセル不可
  - エラー `ORDER_NOT_CANCELLABLE`「この注文はキャンセルできません」
- 既にキャンセル済みの注文は再キャンセル不可
  - エラー `ALREADY_CANCELLED`「この注文は既にキャンセルされています」

**実装箇所**:
- `OrderService.cancelOrder()` - backend/src/main/java/com/example/aiec/service/OrderService.java
- `InventoryService.releaseCommittedReservations()` - backend/src/main/java/com/example/aiec/service/InventoryService.java
- `OrderController.cancelOrder()` - backend/src/main/java/com/example/aiec/controller/OrderController.java

---

## 8. 各状態でできる操作

### 8-1. PENDING 状態でできる操作

**実装済み**:
- ✅ 注文詳細の取得（`GET /api/order/:id`）
- ✅ 注文のキャンセル（`POST /api/order/:id/cancel`）
- ✅ 注文の確認（`POST /api/order/:id/confirm`）- 管理者のみ
- ❌ 注文内容の変更（未実装、Phase 2以降）

**実装箇所**:
- `OrderController.java` - backend/src/main/java/com/example/aiec/controller/OrderController.java
- `OrderService.java` - backend/src/main/java/com/example/aiec/service/OrderService.java

---

### 8-2. CONFIRMED 状態でできる操作

**実装済み**:
- ✅ 注文詳細の取得（`GET /api/order/:id`）
- ✅ 注文のキャンセル（`POST /api/order/:id/cancel`）
- ✅ 注文の発送（`POST /api/order/:id/ship`）- 管理者のみ

---

### 8-3. SHIPPED 状態でできる操作

**実装済み**:
- ✅ 注文詳細の取得（`GET /api/order/:id`）
- ✅ 配達完了（`POST /api/order/:id/deliver`）- 管理者のみ
- ❌ キャンセル不可

---

### 8-4. DELIVERED 状態でできる操作

**実装済み**:
- ✅ 注文詳細の取得（`GET /api/order/:id`）
- ❌ ステータス変更不可（最終状態）

---

### 8-5. CANCELLED 状態でできる操作

**実装済み**:
- ✅ 注文詳細の取得（`GET /api/order/:id`）
- ❌ ステータス変更不可（終端状態）

---

## 9. 注文データの永続化

### 9-1. 注文データの保存先

**Given**: 注文が作成される
**When**: データベースに保存される
**Then**:
- 保存先: SQLite データベース（`/app/data/ec.db`）
- テーブル: `orders` テーブル
- 関連テーブル: `order_items` テーブル

**実装箇所**:
- バックエンド: `Order.java`（エンティティ定義）
- バックエンド: `OrderItem.java`（注文商品エンティティ）

---

### 9-2. 注文データの永続性

**Given**: 注文が作成される
**When**: アプリケーションを再起動する
**Then**:
- 注文データは保持される
- 注文番号、商品リスト、合計金額、ステータスなどはすべて永続化される
- データベースファイルが存在する限りデータは失われない

**検証方法**:
1. 注文を作成
2. バックエンドコンテナを再起動（`docker restart ai-ec-backend`）
3. 注文詳細取得API（`GET /api/order/:id`）で確認
4. データが保持されていることを確認

---

## 10. エッジケースと制約

### 10-1. 注文作成の同時実行

**問題点**:
- 在庫チェックと注文作成の間にタイムラグがある
- 複数ユーザーが同時に同じ商品を注文した場合、在庫の競合が発生する可能性

**現在の実装**:
- トランザクション制御: あり（`@Transactional`）
- 排他制御: なし
- 在庫引当: なし（チェックのみ）

**影響**:
- 理論上、在庫数を超える注文が可能

---

### 10-2. 注文番号の重複

**対策**:
- データベースレベルで一意制約（`unique = true`）
- 重複が発生した場合はエラーになる

**実装箇所**:
- バックエンド: `Order.java:26-27`

---

### 10-3. 注文作成後の在庫変動

**Given**:
- 注文を作成した（商品A × 5個）
- 注文作成後、管理画面で商品Aの在庫を3個に変更

**When**: 特に何も起きない
**Then**:
- 注文データは変更されない
- 注文作成時点の商品情報が保持される
- 在庫変更が注文に影響しない

**理由**:
- 注文商品（`OrderItem`）は注文時点の商品情報を持つ
- 商品マスタ（`Product`）とは独立している

---

## 11. 配送料・手数料の扱い

### 11-1. 配送料・手数料は常に¥0

**Given**: 注文を作成する
**When**: 合計金額を計算する
**Then**:
- 配送料: ¥0（固定）
- 手数料: ¥0（固定）
- 合計金額 = 商品小計 + ¥0 + ¥0

**実装箇所**:
- フロントエンド: `OrderConfirmPage.tsx:125-131`
- バックエンド: 配送料・手数料の計算ロジックは存在しない

**注意**:
- 仕様書（SPEC.md）にも配送料・手数料の計算ロジックの記載なし
- ギャップ分析レポート `docs/gap-analysis.md` の「2-1, 3-3」で指摘済み

---

## 12. 今後の実装予定

以下の機能は仕様書に記載があるが、現在未実装：

### 12-1. 注文状態遷移機能
- PENDING → CONFIRMED → SHIPPED → DELIVERED の遷移
- 各遷移時の処理（在庫引当、決済、通知メール等）

### 12-2. 注文キャンセル機能
- 注文ステータスを CANCELLED に変更
- 在庫の戻し処理
- 決済キャンセル

### 12-3. 在庫引当処理
- 注文作成時に `stock` を減少させる
- トランザクション管理と排他制御

### 12-4. 注文履歴機能
- 自分の過去の注文一覧を取得
- 注文の検索・フィルタリング

### 12-5. 配送情報管理
- 配送先住所の登録
- 配送状況の追跡
- 追跡番号の管理

---

## 13. 参考情報

### 関連仕様書
- `docs/SPEC.md`: 機能仕様書（注文機能: 213-235行目、状態遷移: 284-338行目）
- `docs/api-spec.md`: API仕様書（注文API: 370-514行目）
- `docs/gap-analysis.md`: ギャップ分析レポート（注文関連の問題点）
- `docs/specs/inventory.md`: 在庫管理仕様書（在庫引当との関連）

### 関連実装
- **フロントエンド**:
  - `frontend/src/pages/OrderConfirmPage.tsx`: 注文確認画面
  - `frontend/src/pages/OrderCompletePage.tsx`: 注文完了画面
  - `frontend/src/contexts/CartContext.tsx`: カート管理

- **バックエンド**:
  - `backend/src/main/java/com/example/aiec/service/OrderService.java`: 注文サービス
  - `backend/src/main/java/com/example/aiec/controller/OrderController.java`: 注文コントローラ
  - `backend/src/main/java/com/example/aiec/entity/Order.java`: 注文エンティティ
  - `backend/src/main/java/com/example/aiec/entity/OrderItem.java`: 注文商品エンティティ

---

## 変更履歴

| バージョン | 日付 | 変更内容 | 作成者 |
|-----------|------|---------|--------|
| 1.0 | 2026-02-10 | 初版作成 | Claude Sonnet 4.5 |
