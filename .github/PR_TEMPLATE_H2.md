## 概要
spec-implementation-gaps.md の **H-2（優先度: 高）** に対応

注文時に在庫数を減らす処理がなく、オーバーセリングのリスクがある問題を修正しました。
仮引当（TENTATIVE）・本引当（COMMITTED）の2段階方式で在庫引当を実装しています。

## 修正内容

### 新規ファイル（10ファイル）

| ファイル | 概要 |
|---------|------|
| `entity/StockReservation.java` | 在庫引当エンティティ（ReservationType enum含む） |
| `repository/StockReservationRepository.java` | JPQL クエリ（有効在庫計算、期限切れ削除等） |
| `exception/ConflictException.java` | 409 Conflict 用例外 |
| `dto/CreateReservationRequest.java` | 仮引当作成リクエスト |
| `dto/ReservationDto.java` | 引当レスポンス |
| `dto/AvailabilityDto.java` | 有効在庫レスポンス |
| `service/InventoryService.java` | 在庫引当ビジネスロジック |
| `controller/InventoryController.java` | 在庫引当API（5エンドポイント） |
| `config/SchedulingConfig.java` | @EnableScheduling |
| `.github/PR_TEMPLATE_H2.md` | このPRテンプレート |

### 既存修正ファイル（3ファイル）

| ファイル | 変更内容 |
|---------|---------|
| `exception/GlobalExceptionHandler.java` | ConflictException ハンドラ追加（409） |
| `service/CartService.java` | 在庫チェックを InventoryService 経由に変更 |
| `service/OrderService.java` | 本引当 commit + stock 減少ロジック統合、cancelOrder 追加 |

## 準拠仕様

✅ **docs/specs/inventory.md セクション4〜14**: 在庫引当の仕様

## 在庫引当の仕組み

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

**有効在庫** = `products.stock` − 有効な仮引当合計 − 本引当合計

## 動作確認結果

### ✅ テスト1: 仮引当作成（カート追加）
```bash
# カート追加
$ curl -X POST /api/order/cart/items -H "X-Session-Id: test" -d '{"productId":1,"quantity":2}'
→ success: true, quantity: 2

# 有効在庫確認
$ curl /api/inventory/availability/1
→ physicalStock: 10, tentativeReserved: 2, availableStock: 8
```

### ✅ テスト2: 仮引当更新（カート数量変更）
```bash
$ curl -X PUT /api/order/cart/items/1 -H "X-Session-Id: test" -d '{"quantity":4}'
→ success: true, quantity: 4

$ curl /api/inventory/availability/1
→ tentativeReserved: 4, availableStock: 6
```

### ✅ テスト3: 本引当（注文確定）
```bash
$ curl -X POST /api/order -H "X-Session-Id: test" -d '{"cartId":"test"}'
→ success: true, stock: 6（10→6に減少）

$ curl /api/inventory/availability/1
→ physicalStock: 6, committedReserved: 4, availableStock: 2
```

### ✅ テスト4: 本引当解除（注文キャンセル）
```bash
$ curl -X POST /api/inventory/reservations/release -H "X-Session-Id: test" -d '{"orderId":4}'
→ success: true

$ curl /api/inventory/availability/1
→ physicalStock: 10, committedReserved: 0, availableStock: 10（stock復帰）
```

### ✅ テスト5: 在庫不足エラー
```bash
$ curl -X POST /api/order/cart/items -H "X-Session-Id: test2" -d '{"productId":1,"quantity":11}'
→ success: false, error: INSUFFICIENT_STOCK（409 Conflict）
```

### 動作確認サマリー

| テストケース | 結果 | 検証内容 |
|------------|------|---------|
| 1. 仮引当作成 | ✅ 成功 | カート追加時に仮引当レコード作成 |
| 2. 仮引当更新 | ✅ 成功 | カート数量変更時に仮引当更新 |
| 3. 本引当（注文確定） | ✅ 成功 | stock減少 + 仮引当→本引当変換 |
| 4. 本引当解除（キャンセル） | ✅ 成功 | stock復帰 + 本引当削除 |
| 5. 在庫不足エラー | ✅ 成功 | 有効在庫超過時に409 Conflict |

**検証環境**: Docker環境（backend: 2026-02-10）

## 新規API

| メソッド | パス | 説明 |
|---------|------|------|
| POST | `/api/inventory/reservations` | 仮引当作成 |
| DELETE | `/api/inventory/reservations` | 仮引当解除 |
| POST | `/api/inventory/reservations/commit` | 本引当 |
| POST | `/api/inventory/reservations/release` | 本引当解除 |
| GET | `/api/inventory/availability/{productId}` | 有効在庫確認 |

## 影響範囲

### 在庫管理
- ✅ オーバーセリング防止（有効在庫ベースのチェック）
- ✅ 注文確定時に確実にstock減少
- ✅ キャンセル時にstock復帰

### API互換性
- ⚠️ カート追加時のエラーコードが `OUT_OF_STOCK` → `INSUFFICIENT_STOCK` に変更
- ⚠️ 在庫チェックが有効在庫ベースに変更（他ユーザーの仮引当を考慮）

### 定期処理
- 5分ごとに期限切れの仮引当をクリーンアップ
- 仮引当のデフォルト有効期限: 30分

## 関連課題

- spec-implementation-gaps.md H-2（優先度: 高）
- docs/specs/inventory.md セクション4〜14

🤖 Generated with Claude Code
