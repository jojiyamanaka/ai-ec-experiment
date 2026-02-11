## 概要
spec-implementation-gaps.md の **H-3（優先度: 高）** に対応

注文状態遷移機能（PENDING → CONFIRMED → SHIPPED → DELIVERED）とキャンセル機能が未実装だった問題を修正しました。

## 修正内容

### バックエンド実装

#### 1. InventoryService の拡張
- **ファイル**: `backend/src/main/java/com/example/aiec/service/InventoryService.java`
- **変更**: `releaseCommittedReservations()` に既にキャンセル済みチェックを追加
  - `CANCELLED` 状態の注文は再キャンセル不可
  - エラーコード: `ALREADY_CANCELLED`

#### 2. OrderService の拡張
- **ファイル**: `backend/src/main/java/com/example/aiec/service/OrderService.java`
- **変更1**: `cancelOrder()` の戻り値を `void` から `OrderDto` に変更
- **変更2**: 状態遷移メソッドを追加
  - `confirmOrder(Long orderId)` - PENDING → CONFIRMED
  - `shipOrder(Long orderId)` - CONFIRMED → SHIPPED
  - `deliverOrder(Long orderId)` - SHIPPED → DELIVERED
  - `getAllOrders()` - 全注文取得（管理者用）

#### 3. OrderController の拡張
- **ファイル**: `backend/src/main/java/com/example/aiec/controller/OrderController.java`
- **変更**: 5つの新規エンドポイントを追加
  - `POST /api/order/:id/cancel` - 注文キャンセル（顧客向け）
  - `POST /api/order/:id/confirm` - 注文確認（管理者向け）
  - `POST /api/order/:id/ship` - 注文発送（管理者向け）
  - `POST /api/order/:id/deliver` - 注文配達完了（管理者向け）
  - `GET /api/order` - 全注文取得（管理者向け）

### フロントエンド実装

#### 1. API層の拡張
- **ファイル**: `frontend/src/lib/api.ts`
- **変更**: 注文状態遷移APIの関数を追加
  - `cancelOrder(orderId)`, `confirmOrder(orderId)`, `shipOrder(orderId)`, `deliverOrder(orderId)`, `getAllOrders()`

#### 2. 既存ページの拡張
- **OrderConfirmPage.tsx**: 注文作成後に `orderId` と `status` を state に追加
- **OrderCompletePage.tsx**: キャンセルボタンとロジックを追加（PENDING 状態のみ表示）

#### 3. 新規ページの追加
- **OrderDetailPage.tsx** (`/order/:id`):
  - 注文詳細表示
  - ステータスバッジ（PENDING/CONFIRMED/SHIPPED/DELIVERED/CANCELLED）
  - キャンセルボタン（PENDING/CONFIRMED のみ表示）

- **AdminOrderPage.tsx** (`/bo/order`):
  - 全注文一覧表示
  - ステータスフィルタ機能
  - 状態遷移ボタン（確認・発送・配達完了・キャンセル）

#### 4. ルーティング
- **App.tsx**: `/order/:id` と `/bo/order` のルートを追加

### ドキュメント更新

#### 1. API仕様書
- **ファイル**: `docs/api-spec.md`
- **変更**: 注文状態遷移APIのエンドポイント仕様を追記（10〜14節）
- **エラーコード追加**: `ORDER_NOT_CANCELLABLE`, `ALREADY_CANCELLED`, `INVALID_STATUS_TRANSITION`

#### 2. ギャップ分析レポート
- **ファイル**: `docs/spec-implementation-gaps.md`
- **変更**: H-3 と M-4 を「✅ 実装完了」に更新
- **サマリー**: 優先度「高」が 3件→1件 に減少

#### 3. 詳細仕様書
- **order.md**: セクション7「注文の状態遷移」を「未実装」から「✅ 実装済み」に更新
- **admin.md**: セクション5「注文管理画面」を追加
- **inventory.md**: セクション4-6「本引当の解除」に実装済みステータスを追記

## 準拠仕様

✅ **SPEC.md:284-338**: 注文の状態遷移（PENDING → CONFIRMED → SHIPPED → DELIVERED）
✅ **SPEC.md:329-332**: PENDING/CONFIRMED → CANCELLED への遷移
✅ **order.md:294-378**: 注文状態遷移の詳細仕様

## Given/When/Then 検証

### 検証 1: 注文確認（PENDING → CONFIRMED）
**Given**: 注文ID=1のステータスが `PENDING`
**When**: `POST /api/order/1/confirm` を呼び出す
**Then**:
- ✅ 注文ステータスが `CONFIRMED` に変更される
- ✅ `updatedAt` が更新される
- ✅ 変更後の注文データが返される

### 検証 2: 注文発送（CONFIRMED → SHIPPED）
**Given**: 注文ID=1のステータスが `CONFIRMED`
**When**: `POST /api/order/1/ship` を呼び出す
**Then**:
- ✅ 注文ステータスが `SHIPPED` に変更される
- ✅ 変更後の注文データが返される

### 検証 3: 配達完了（SHIPPED → DELIVERED）
**Given**: 注文ID=1のステータスが `SHIPPED`
**When**: `POST /api/order/1/deliver` を呼び出す
**Then**:
- ✅ 注文ステータスが `DELIVERED` に変更される

### 検証 4: 注文キャンセル（PENDING → CANCELLED）
**Given**: 注文ID=2のステータスが `PENDING`、本引当レコードが存在
**When**: `POST /api/order/2/cancel` を呼び出す（X-Session-Id ヘッダー付き）
**Then**:
- ✅ 注文ステータスが `CANCELLED` に変更される
- ✅ 本引当レコード（`stock_reservations`）が削除される
- ✅ `products.stock` が引当数量分だけ増加（在庫が戻る）

### 検証 5: 不正な状態遷移のバリデーション
**Given**: 注文ID=1のステータスが `PENDING`
**When**: `POST /api/order/1/ship` を呼び出す（CONFIRMED をスキップ）
**Then**:
- ✅ エラーコード `INVALID_STATUS_TRANSITION` が返される
- ✅ エラーメッセージ「この注文は発送できません（現在のステータス: PENDING）」が返される

### 検証 6: キャンセル不可の状態でキャンセル試行
**Given**: 注文ID=1のステータスが `SHIPPED`
**When**: `POST /api/order/1/cancel` を呼び出す
**Then**:
- ✅ エラーコード `ORDER_NOT_CANCELLABLE` が返される
- ✅ エラーメッセージ「この注文はキャンセルできません」が返される

### 検証 7: 既にキャンセル済みの注文をキャンセル試行
**Given**: 注文ID=2のステータスが `CANCELLED`
**When**: `POST /api/order/2/cancel` を呼び出す
**Then**:
- ✅ エラーコード `ALREADY_CANCELLED` が返される
- ✅ エラーメッセージ「この注文は既にキャンセルされています」が返される

## 動作確認手順

### 前提条件
```bash
# Docker起動
docker compose up -d

# ログ確認
docker compose logs -f backend
```

### バックエンドAPIテスト

#### テスト 1: 注文作成
```bash
# カートに商品追加
curl -X POST http://localhost:8080/api/order/cart/items \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session-123" \
  -d '{"productId": 1, "quantity": 2}'

# 注文確定
curl -X POST http://localhost:8080/api/order \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session-123" \
  -d '{"cartId": "test-session-123"}'
```
**期待結果**: `{"success":true,"data":{"orderId":1,"orderNumber":"ORD-...", "status":"PENDING",...}}`

#### テスト 2: 注文確認（PENDING → CONFIRMED）
```bash
curl -X POST http://localhost:8080/api/order/1/confirm
```
**期待結果**: `{"success":true,"data":{..."status":"CONFIRMED"...}}`

#### テスト 3: 注文発送（CONFIRMED → SHIPPED）
```bash
curl -X POST http://localhost:8080/api/order/1/ship
```
**期待結果**: `{"success":true,"data":{..."status":"SHIPPED"...}}`

#### テスト 4: 配達完了（SHIPPED → DELIVERED）
```bash
curl -X POST http://localhost:8080/api/order/1/deliver
```
**期待結果**: `{"success":true,"data":{..."status":"DELIVERED"...}}`

#### テスト 5: 注文キャンセル（新規注文を作成してテスト）
```bash
# 新規注文作成（test-session-456）
curl -X POST http://localhost:8080/api/order/cart/items \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session-456" \
  -d '{"productId": 2, "quantity": 1}'

curl -X POST http://localhost:8080/api/order \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session-456" \
  -d '{"cartId": "test-session-456"}'

# 注文キャンセル（orderId=2 と仮定）
curl -X POST http://localhost:8080/api/order/2/cancel \
  -H "X-Session-Id: test-session-456"
```
**期待結果**: `{"success":true,"data":{..."status":"CANCELLED"...}}`

#### テスト 6: 全注文取得
```bash
curl http://localhost:8080/api/order
```
**期待結果**: `{"success":true,"data":[{注文1},{注文2},...]}`

#### テスト 7: エラーケース - 不正な状態遷移
```bash
# PENDING から直接 SHIPPED へ遷移試行
curl -X POST http://localhost:8080/api/order/cart/items \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session-789" \
  -d '{"productId": 1, "quantity": 1}'

curl -X POST http://localhost:8080/api/order \
  -H "Content-Type: application/json" \
  -H "X-Session-Id: test-session-789" \
  -d '{"cartId": "test-session-789"}'

# orderId=3 と仮定
curl -X POST http://localhost:8080/api/order/3/ship
```
**期待結果**: `{"success":false,"error":{"code":"INVALID_STATUS_TRANSITION","message":"この注文は発送できません（現在のステータス: PENDING）"}}`

#### テスト 8: エラーケース - SHIPPED 状態でキャンセル試行
```bash
# orderId=1（既に DELIVERED になっている）をキャンセル試行
curl -X POST http://localhost:8080/api/order/1/cancel \
  -H "X-Session-Id: test-session-123"
```
**期待結果**: `{"success":false,"error":{"code":"ORDER_NOT_CANCELLABLE","message":"この注文はキャンセルできません"}}`

### フロントエンド動作確認

#### 確認 1: 注文完了画面のキャンセル機能
1. http://localhost:5173 にアクセス
2. 商品をカートに追加
3. 注文確認画面で注文確定
4. 注文完了画面で「注文をキャンセル」ボタンが表示されることを確認
5. ボタンをクリックしてキャンセル実行
6. **期待結果**: キャンセル成功のアラート、ボタンが非表示になる

#### 確認 2: 注文詳細画面
1. http://localhost:5173/order/1 にアクセス
2. **期待結果**:
   - 注文情報が表示される
   - ステータスバッジが表示される（色分け）
   - PENDING/CONFIRMED 状態ならキャンセルボタンが表示される
   - SHIPPED 以降はキャンセルボタンが非表示

#### 確認 3: 管理画面 - 注文管理
1. http://localhost:5173/bo/order にアクセス
2. **期待結果**:
   - 全注文が一覧表示される
   - ステータスフィルタが動作する
   - PENDING 状態の注文に「確認」「キャンセル」ボタンが表示される
   - CONFIRMED 状態の注文に「発送」「キャンセル」ボタンが表示される
   - SHIPPED 状態の注文に「配達完了」ボタンが表示される
3. 「確認」ボタンをクリック
4. **期待結果**: ステータスが CONFIRMED に変更、画面が自動更新
5. 「発送」ボタンをクリック
6. **期待結果**: ステータスが SHIPPED に変更
7. 「配達完了」ボタンをクリック
8. **期待結果**: ステータスが DELIVERED に変更、アクションボタンが非表示

---

### 動作確認サマリー

| テストケース | 結果 | 検証内容 |
|------------|------|---------|
| 1. 注文作成 | ✅ | PENDING ステータスで作成 |
| 2. 注文確認 | ✅ | PENDING → CONFIRMED |
| 3. 注文発送 | ✅ | CONFIRMED → SHIPPED |
| 4. 配達完了 | ✅ | SHIPPED → DELIVERED |
| 5. 注文キャンセル | ✅ | PENDING/CONFIRMED → CANCELLED（在庫戻し） |
| 6. 全注文取得 | ✅ | 管理者向けAPI |
| 7. 不正な状態遷移 | ✅ | バリデーションエラー |
| 8. SHIPPED でキャンセル | ✅ | キャンセル不可エラー |
| 9. 注文完了画面キャンセル | ✅ | フロントエンド動作確認 |
| 10. 注文詳細画面 | ✅ | ステータスバッジ、キャンセルボタン |
| 11. 管理画面 - 注文管理 | ✅ | 一覧表示、状態遷移、フィルタ |

**検証環境**: Docker環境（backend + frontend）

## 影響範囲

### 機能追加
- ✅ 注文状態遷移機能（PENDING → CONFIRMED → SHIPPED → DELIVERED）
- ✅ 注文キャンセル機能（顧客向け・管理者向け）
- ✅ 管理画面 - 注文管理ページ
- ✅ 注文詳細画面

### API追加
- ✅ 5つの新規エンドポイント（cancel, confirm, ship, deliver, getAllOrders）

### データ整合性
- ✅ 注文キャンセル時に在庫が自動的に戻る
- ✅ 不正な状態遷移をバリデーション
- ✅ 既にキャンセル済みの注文は再キャンセル不可

### 後方互換性
- ⚠️ **破壊的変更なし**: 既存APIの動作は変更なし
- ✅ OrderService.cancelOrder() の戻り値変更（void → OrderDto）は内部メソッドのため影響なし

## 関連課題

- spec-implementation-gaps.md H-3（優先度: 高）- 注文状態遷移機能が未実装
- spec-implementation-gaps.md M-4（優先度: 中）- 注文キャンセル機能が未実装

## 今後の拡張予定（Phase 2以降）

- 追跡番号の発行（SHIPPED 時）
- 発送通知メール送信
- 配達完了通知メール送信
- 決済処理との連携
- キャンセル通知メール送信

🤖 Generated with Claude Code
