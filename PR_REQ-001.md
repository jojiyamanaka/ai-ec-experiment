# 在庫不足時の詳細エラーレスポンス実装（REQ-001）

## 概要

注文確定時の在庫不足エラー（OUT_OF_STOCK）に商品別詳細情報を追加し、ユーザー体験を向上させます。

**関連Issue/要件**:
- 要件定義: `docs/01_requirements/REQ-001_在庫不足詳細エラー.md`
- 実装計画: `docs/02_designs/PLAN-001_在庫不足詳細エラー.md`
- ギャップ分析: `docs/spec-implementation-gaps.md` (M-2)

## 変更内容

### 🔧 バックエンド

#### 新規追加
- **`StockShortageDetail.java`** - 在庫不足商品の詳細情報を保持するDTO
  - フィールド: `productId`, `productName`, `requestedQuantity`, `availableStock`

- **`InsufficientStockException.java`** - 詳細情報付き在庫不足例外
  - `ConflictException` を継承
  - `List<StockShortageDetail>` を保持

#### 修正
- **`InventoryService.java`**
  - `commitReservations()` メソッドを修正
  - 全商品の在庫チェック後、不足商品リストを収集してから例外をスロー
  - 複数商品が在庫不足の場合も一度にすべて検出

- **`ApiResponse.java`**
  - `errorWithDetails()` 静的メソッドを追加
  - `ErrorDetail` に `details` フィールド（Optional）を追加

- **`GlobalExceptionHandler.java`**
  - `InsufficientStockException` ハンドラーを追加
  - HTTPステータス 409 Conflict で詳細情報付きレスポンスを返す

### 🎨 フロントエンド

#### 型定義
- **`api.ts`**
  - `StockShortageDetail` インターフェースを追加
  - `ApiError` インターフェースを追加（`details` フィールド付き）
  - `ApiResponse` の `error` フィールドを `ApiError` 型に変更

#### UI
- **`OrderConfirmPage.tsx`**
  - エラー状態管理を追加
  - OUT_OF_STOCK エラー時に商品別詳細リストを表示
  - 各商品について「商品名（要求: XX個、在庫: YY個）」と表示
  - カートに戻るよう促すメッセージを表示

## レスポンス例

### 在庫不足エラー（詳細情報付き）

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
      },
      {
        "productId": 2,
        "productName": "スマートウォッチ",
        "requestedQuantity": 5,
        "availableStock": 2
      }
    ]
  }
}
```

## 動作確認

### ✅ 確認済み項目
- [x] バックエンドのコンパイルが通る
- [x] 既存の `error()` メソッドは影響を受けない
- [x] `InsufficientStockException` は `ConflictException` より優先して処理される
- [x] HTTPステータスコード 409 が返される

### 📋 手動テスト手順
1. 商品をカートに追加
2. カート内の数量を在庫数以上に変更（またはDBで在庫数を減らす）
3. 注文確定画面で「注文を確定する」をクリック
4. エラーメッセージと商品別詳細が表示されることを確認

### 🧪 テストケース（今後追加予定）
- [ ] 単体テスト: `InventoryServiceTest` - 在庫不足商品の検出
- [ ] 統合テスト: `OrderControllerTest` - エラーレスポンスの形式
- [ ] E2Eテスト: 在庫不足エラーの表示確認

## 影響範囲

### 後方互換性
✅ **互換性あり**
- 既存の `ConflictException` を使用しているエラーレスポンスは影響を受けません
- `errorWithDetails()` は新規メソッドで、既存の `error()` メソッドは維持

### 依存関係
- なし（既存の在庫引当システムを活用）

### パフォーマンス
- 在庫チェックを全商品分実行するため、わずかなオーバーヘッドあり
- 注文確定時の1回のみなので、実用上問題なし

## スクリーンショット（任意）

<!-- ここにエラー表示のスクリーンショットを追加 -->

## チェックリスト

- [x] コードがコンパイル可能
- [x] 既存機能に影響がないことを確認
- [x] エラーレスポンスの形式が仕様通り
- [ ] 手動テストで動作確認
- [ ] コードレビューの指摘事項に対応
- [ ] ドキュメント更新（該当する場合）

## レビュー観点

- [ ] `InsufficientStockException` の設計は適切か
- [ ] エラーメッセージは分かりやすいか
- [ ] フロントエンドのUI表示は適切か
- [ ] 複数商品が在庫不足の場合も正しく動作するか

## 備考

- 本PRは `docs/spec-implementation-gaps.md` の M-2（在庫不足時の詳細エラーレスポンス）に対応
- 要件定義書と実装計画書は別途 `docs/01_requirements/` と `docs/02_designs/` に配置
- 見積もり工数: 10時間（約1.5日）
