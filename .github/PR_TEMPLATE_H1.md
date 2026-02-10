## 概要
spec-implementation-gaps.md の **H-1（優先度: 高）** に対応

非公開商品（`isPublished = false`）が商品詳細API（`GET /api/item/:id`）でIDさえ分かれば取得できてしまうセキュリティ問題を修正しました。

## 修正内容

### コード修正
- **ファイル**: `backend/src/main/java/com/example/aiec/service/ProductService.java`
- **変更**: `getProductById` メソッドに非公開商品のチェックを追加
  - 非公開商品の場合、`ResourceNotFoundException` をスロー
  - エラーコード: `ITEM_NOT_FOUND`
  - エラーメッセージ: "商品が見つかりません"

### 仕様書修正
- **ファイル**: `docs/specs/admin.md`
- **変更**: シナリオ 3-1 を修正
  - 旧: 商品詳細APIは非公開商品も取得可能
  - 新: 商品詳細APIでも非公開商品はアクセス不可

## 準拠仕様

✅ **SPEC.md:179**: 「非公開（isPublished = false）の商品は表示しない」（商品詳細画面を含む）

## Given/When/Then 検証

### 検証 1: SPEC.md の要件
**Given**: 商品ID=1の商品が非公開（isPublished = false）
**When**: `GET /api/item/1` を呼び出す
**Then**:
- ✅ エラーコード `ITEM_NOT_FOUND` が返される
- ✅ エラーメッセージ「商品が見つかりません」が返される
- ✅ 商品情報は返されない

### 検証 2: admin.md シナリオ 3-1
**Given**: 商品E（公開中）が商品一覧に表示されている
**When**: 管理者が商品Eを非公開に変更
**Then**:
- ✅ 商品一覧画面から商品Eが消える（既存実装）
- ✅ 商品詳細画面でもアクセス不可になる（**今回の修正**）
- ✅ 直接URLを知っていても「商品が見つかりません」エラーが返される（**今回の修正**）

## 動作確認結果

### ✅ テストケース 1: 公開商品は取得できる
```bash
$ curl http://localhost:8080/api/item/1
{"success":true,"data":{"id":1,"name":"ワイヤレスイヤホン","price":7980,"image":"https://placehold.co/400x300/3b82f6/ffffff?text=Product+1","description":"高音質で長時間バッテリー対応のワイヤレスイヤホン","stock":10,"isPublished":true}}
```
**結果**: ✅ 成功 - 公開商品が正常に取得できる

### ✅ テストケース 2: 商品を非公開に変更
```bash
$ curl -X PUT http://localhost:8080/api/item/1 \
  -H "Content-Type: application/json" \
  -d '{"isPublished": false}'
{"success":true,"data":{...,"isPublished":false}}
```
**結果**: ✅ 成功 - 商品が非公開に変更される

### ✅ テストケース 3: 非公開商品は取得できない（🔑 修正の検証）
```bash
$ curl http://localhost:8080/api/item/1
{"success":false,"error":{"code":"ITEM_NOT_FOUND","message":"商品が見つかりません"}}
```
**結果**: ✅ **成功 - 非公開商品は取得できず、期待通りのエラーが返される**

### ✅ テストケース 4: 商品を公開に戻す
```bash
$ curl -X PUT http://localhost:8080/api/item/1 \
  -H "Content-Type: application/json" \
  -d '{"isPublished": true}'

$ curl http://localhost:8080/api/item/1
{"success":true,"data":{...,"isPublished":true}}
```
**結果**: ✅ 成功 - 商品を公開に戻すと再び取得可能になる

---

### 動作確認サマリー

| テストケース | 結果 | 検証内容 |
|------------|------|---------|
| 1. 公開商品の取得 | ✅ 成功 | 通常の動作確認 |
| 2. 商品を非公開に変更 | ✅ 成功 | 管理画面の動作確認 |
| 3. **非公開商品の取得** | ✅ **成功** | **修正内容の検証（エラー返却）** |
| 4. 商品を公開に戻す | ✅ 成功 | 元に戻せることを確認 |

**検証環境**: Docker環境（backend: 2026-02-10 15:12:xx 起動）

## 影響範囲

### セキュリティ
- ✅ 非公開商品の情報漏洩を防止

### API互換性
- ⚠️ **破壊的変更**: 非公開商品の詳細取得が失敗するようになる
  - ただし、本来あるべき仕様に準拠

### フロントエンド
- 影響なし（商品一覧から非公開商品は既に除外されている）

## 関連課題

- spec-implementation-gaps.md H-1（優先度: 高）

🤖 Generated with Claude Code
