# H-3 注文状態遷移機能 テストガイド

## 前提条件

1. Docker Desktop が起動していること
2. コンテナが起動していること

```bash
docker compose up -d
```

## 自動テストの実行

### Linux/Mac/WSL
```bash
chmod +x test-h3-order-status.sh
./test-h3-order-status.sh
```

### Windows (PowerShell)
```powershell
.\test-h3-order-status.ps1
```

## テスト内容

以下の9つのテストケースを自動実行します:

1. **注文作成（PENDING）** - 新規注文が PENDING ステータスで作成される
2. **注文確認（PENDING → CONFIRMED）** - 管理者が注文を確認
3. **注文発送（CONFIRMED → SHIPPED）** - 管理者が注文を発送
4. **配達完了（SHIPPED → DELIVERED）** - 配達業者が配達完了
5. **注文キャンセル（PENDING → CANCELLED）** - 顧客が注文をキャンセル（在庫戻し）
6. **全注文取得** - 管理者向けAPI
7. **不正な状態遷移の拒否** - PENDING から直接 SHIPPED へ遷移試行 → エラー
8. **DELIVERED でキャンセル拒否** - 配達完了後はキャンセル不可 → エラー
9. **既にキャンセル済みをキャンセル拒否** - 二重キャンセル不可 → エラー

## 期待される出力

```
==========================================
H-3 注文状態遷移機能 テスト
==========================================

📋 テスト環境:
  - BASE_URL: http://localhost:8080
  - SESSION_ID_1: test-session-...
  ...

==========================================
テスト 1: 注文作成（PENDING）
==========================================
カートに商品追加...
✅ PASS

注文作成...
✅ 注文作成成功（orderId: 1, status: PENDING）

==========================================
テスト 2: 注文確認（PENDING → CONFIRMED）
==========================================
✅ 注文確認成功（status: CONFIRMED）

... (以下続く)

==========================================
✅ 全テスト完了
==========================================

テスト結果サマリー:
  1. 注文作成（PENDING）: ✅
  2. 注文確認（PENDING → CONFIRMED）: ✅
  3. 注文発送（CONFIRMED → SHIPPED）: ✅
  4. 配達完了（SHIPPED → DELIVERED）: ✅
  5. 注文キャンセル（PENDING → CANCELLED）: ✅
  6. 全注文取得: ✅
  7. 不正な状態遷移の拒否: ✅
  8. DELIVERED でキャンセル拒否: ✅
  9. 既にキャンセル済みをキャンセル拒否: ✅

🎉 すべてのテストが成功しました！
```

## 手動テスト（フロントエンド）

### 1. 注文完了画面のキャンセル機能

1. http://localhost:5173 にアクセス
2. 商品をカートに追加
3. 注文確認画面で注文確定
4. 注文完了画面で「注文をキャンセル」ボタンをクリック
5. **確認**: キャンセル成功のアラート、ボタンが非表示になる

### 2. 注文詳細画面

1. http://localhost:5173/order/1 にアクセス
2. **確認**:
   - 注文情報が表示される
   - ステータスバッジが表示される（色分け）
   - PENDING/CONFIRMED 状態ならキャンセルボタンが表示される
   - SHIPPED 以降はキャンセルボタンが非表示

### 3. 管理画面 - 注文管理

1. http://localhost:5173/bo/order にアクセス
2. **確認**:
   - 全注文が一覧表示される
   - ステータスフィルタが動作する
   - 各状態に応じた適切なボタンが表示される

3. **操作テスト**:
   - PENDING 状態の注文で「確認」ボタンをクリック → CONFIRMED に変更
   - CONFIRMED 状態の注文で「発送」ボタンをクリック → SHIPPED に変更
   - SHIPPED 状態の注文で「配達完了」ボタンをクリック → DELIVERED に変更
   - PENDING/CONFIRMED 状態の注文で「キャンセル」ボタンをクリック → CANCELLED に変更

## トラブルシューティング

### Docker が起動していない
```
Error: unable to get image 'ai-ec-experiment-backend'
```
→ Docker Desktop を起動してください

### ポート 8080 が使用中
```
Error: Bind for 0.0.0.0:8080 failed: port is already allocated
```
→ 他のプロセスがポート 8080 を使用しています。Docker を再起動するか、該当プロセスを終了してください

### テストが失敗する
1. コンテナのログを確認
   ```bash
   docker compose logs -f backend
   ```

2. データベースをリセット
   ```bash
   docker compose down -v
   docker compose up -d
   ```

3. テストを再実行

## 参考資料

- PRテンプレート: `.github/PR_TEMPLATE_H3.md`
- API仕様書: `docs/api-spec.md`
- 注文管理仕様書: `docs/specs/order.md`
- 管理画面仕様書: `docs/specs/admin.md`
