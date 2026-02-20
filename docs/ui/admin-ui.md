# 管理画面のUI仕様

**目的**: AIを活用したECサイトの管理画面のUI/UX仕様を定義する

**関連ドキュメント**:
- [BackOffice BFF OpenAPI仕様](../api/backoffice-bff-openapi.json) - 管理向けAPIエンドポイント
- [商品ドメイン](../specs/product.md) - 商品管理のビジネスロジック
- [注文ドメイン](../specs/order.md) - 注文管理のビジネスロジック

---

## 概要

| 画面 | パス | 主な操作 |
|------|------|---------|
| BoUser ログイン | /bo/login | 管理者アカウントでログイン |
| 商品管理 | /bo/item | 品番/カテゴリ/価格/在庫/公開状態/公開・販売日時の編集、商品新規登録 |
| 注文管理 | /bo/order | 注文一覧・ステータス変更・キャンセル |
| 在庫管理 | /bo/inventory | 在庫一覧・在庫調整・調整履歴 |
| 会員管理 | /bo/members | 会員一覧・新規登録・詳細/FULL更新・住所管理・有効/無効切替 |

すべての変更は**即時反映**（バッチ処理なし）。

---

## 認証・認可

- **BoUser ログイン（/bo/login）**: 認証不要
- **その他の管理画面**: BoUser 認証必須
- **会員管理**: ADMIN / SUPER_ADMIN 権限必須
- **未ログイン**: `/bo/login` へリダイレクト
- **顧客トークン**: 管理APIアクセス不可（`CUSTOMER_TOKEN_NOT_ALLOWED`）

### BoUser ログイン画面

- メールアドレス・パスワードで認証
- 成功時: BoUser トークンを `localStorage` に保存、商品管理画面へ遷移
- 失敗時/アカウント無効時: エラーメッセージ表示

### 操作履歴

管理者の操作は全て `OperationHistory` に `ADMIN_ACTION` として記録（userId, userEmail, requestPath, details）。

---

## 1. 価格変更の影響

| シナリオ | 動作 |
|---------|------|
| カート内商品の価格変更 | カート合計が即座に更新（Product参照で動的計算） |
| 注文確定後の価格変更 | 既存注文に影響なし（OrderItem.subtotalにスナップショット保存） |

---

## 2. 在庫変更の影響

| シナリオ | 動作 |
|---------|------|
| カート内商品の在庫を減少 | カート内商品は残る。数量変更/注文時に在庫不足エラー |
| カート内商品の在庫を0に | カート内商品は残る。注文時に在庫不足エラー |
| 在庫0→増加 | 即座にカート追加可能 |

---

## 3. 公開/非公開の切替

| シナリオ | 動作 |
|---------|------|
| 公開→非公開 | 商品一覧・詳細から即座に非表示。直接URLでもアクセス不可 |
| カート内商品を非公開/期間外化 | カート再取得時に自動除外され、追加/数量変更も拒否 |
| 非公開→公開 | 商品一覧に即座に表示 |

### 公開/販売期間・カテゴリ運用

- 顧客向け表示: `product.isPublished && category.isPublished && now ∈ [publishStartAt, publishEndAt]`
- 購入可否: `顧客向け表示 && now ∈ [saleStartAt, saleEndAt] && stock > 0`
- `isPublished = false` のカテゴリは商品登録/更新で選択不可（`CATEGORY_INACTIVE`）
- 期間制約違反（公開期間・販売期間の逆転、販売期間が公開期間外）は `INVALID_SCHEDULE`

---

## 4. 変更の反映タイミング

- 保存ボタンクリック → DB即座に保存（`@Transactional`）
- 次のAPIリクエストで新しい値が返される
- 複数商品の一括変更: `Promise.all` で並列実行（各商品が独立トランザクション）

---

## 5. 注文管理画面（/bo/order）

### 注文一覧

- 全注文をテーブル表示（注文番号, 日時, 合計金額, ステータス）
- ステータスフィルタ: ALL, PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED

### ステータス変更

| 現在の状態 | アクション | 遷移先 |
|-----------|-----------|--------|
| PENDING | 「確認」ボタン | CONFIRMED |
| PENDING | 「キャンセル」ボタン | CANCELLED（在庫戻し） |
| CONFIRMED | 「発送」ボタン | SHIPPED |
| CONFIRMED | 「キャンセル」ボタン | CANCELLED（在庫戻し） |
| SHIPPED | 「配達完了」ボタン | DELIVERED |
| SHIPPED/DELIVERED/CANCELLED | — | ステータス変更不可 |

### ステータスバッジ色

PENDING(グレー), CONFIRMED(ブルー), SHIPPED(パープル), DELIVERED(グリーン), CANCELLED(レッド)

---

## 6. 在庫管理画面（/bo/inventory）

### 在庫一覧

- 全商品の在庫情報: 商品ID, 商品名, 物理在庫, 有効在庫, 引当数, 公開状態

### 在庫調整

- `POST /api/inventory/adjust` — 商品選択、増減数入力、理由入力（入荷/廃棄/棚卸差異等）
- 調整前後の在庫数と差分を記録

### 在庫調整履歴

- 過去の調整履歴一覧: 商品名, 調整前後の数量, 差分, 理由, 調整者, 日時

---

## 7. 会員管理画面（/bo/members）

- 会員一覧: 会員ID, メールアドレス, 表示名, 有効/無効状態, 登録日時（ADMIN/SUPER_ADMIN権限）
- 会員新規登録: `email`, `displayName`, `password` を必須に、`memberRank`, `loyaltyPoints`, `deactivationReason`, 住所を任意入力
- 会員詳細/FULL更新: `displayName`, `fullName`, `phoneNumber`, `birthDate`, `newsletterOptIn`, `memberRank`, `loyaltyPoints`, `deactivationReason`, `isActive`, 住所配列を更新
- 住所管理: 追加/更新/削除、デフォルト住所切替（会員内 `isDefault` は最大1件）
- 会員状態変更: 有効/無効切替（無効化で会員のログイン不可）
- 利用API:
  - `POST /api/admin/members`
  - `GET /api/admin/members`
  - `GET /api/admin/members/{id}`
  - `PUT /api/admin/members/{id}`
  - `PUT /api/admin/members/{id}/status`
- 更新禁止項目: `passwordHash`, トークン関連, 監査項目, `lastLoginAt`, `termsAgreedAt`

---

## 8. 制限事項

| 制限 | 説明 |
|------|------|
| 価格変更通知なし | カート内商品の価格変更をユーザーに通知しない |
