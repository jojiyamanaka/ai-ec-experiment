# 業務要件・ビジネスルール

**目的**: AIを活用したECサイトの業務要件とビジネスルールを定義する

**関連ドキュメント**: [技術仕様](./SPEC.md), [データモデル](./data-model.md), [顧客画面](./ui/customer-ui.md), [Customer BFF OpenAPI仕様](./api/customer-bff-openapi.json), [BackOffice BFF OpenAPI仕様](./api/backoffice-bff-openapi.json), [Core API OpenAPI仕様](./api/openapi.json)

---

## 主要機能

### 1. 商品閲覧機能

- 公開表示条件を満たす商品（`products.is_published = true` かつ `product_categories.is_published = true` かつ公開期間内）のみ表示
- 在庫状態は在庫数に応じて自動判定（後述）
- 商品画像はプレースホルダー使用（Phase 1）
- 対象画面: TOP画面、商品一覧、商品詳細

### 2. カート機能

| ルール | 内容 |
|--------|------|
| 追加制限 | 売り切れ商品はカート追加不可 |
| 数量制限 | 最小1個、最大9個 |
| 0個時 | 自動削除 |
| 非購入可能商品 | 商品/カテゴリ非公開、公開期間外、販売期間外はカートから自動除外。追加/数量変更も拒否 |
| 合計金額 | リアルタイム再計算 |
| 配送料・手数料 | ¥0 固定（Phase 1） |

### 3. 注文機能

- 注文番号: `ORD-xxxxxxxxxx` 形式（0埋め10桁連番）
- 注文確定時にカート自動クリア
- 注文時に在庫引当（`REAL`: 同期本引当、`FRAME`: 仮引当後に非同期本引当）
- 詳細: [注文管理](./specs/order.md), [在庫管理](./specs/inventory.md)

### 4. 商品管理機能（管理画面）

- 商品情報の一括編集、商品新規登録、カテゴリ管理、在庫数管理、公開/非公開切替、公開/販売日時設定
- 変更は「保存」ボタンクリック時に即時反映
- 複合検索: keyword/categoryId/isPublished/inSalePeriod/allocationType/stockThreshold/zeroStockOnly で絞り込み可能
- 対象画面: /bo/item, /bo/order
- 詳細: [管理画面](./ui/admin-ui.md)

### 4.1 公開/販売期間ルール

| ルール | 内容 |
|--------|------|
| 表示可否 | `product.isPublished && category.isPublished && now ∈ [publishStartAt, publishEndAt]` |
| 購入可否 | `表示可否 && now ∈ [saleStartAt, saleEndAt] && effectiveStock > 0` |
| 整合制約 | `publishStartAt <= publishEndAt`、`saleStartAt <= saleEndAt`、販売期間は公開期間内 |
| バリデーションエラー | 期間制約違反は `INVALID_SCHEDULE` |
| カテゴリ選択制約 | `is_published = false` のカテゴリは商品登録/更新で指定不可（`CATEGORY_INACTIVE`） |

### 5. 会員・住所管理機能

| ルール | 内容 |
|--------|------|
| 会員プロフィール更新（顧客） | `displayName`, `fullName`, `phoneNumber`, `birthDate`, `newsletterOptIn` のみ更新可能 |
| 会員運用項目（管理） | `memberRank`, `loyaltyPoints`, `deactivationReason`, `isActive` は管理画面で更新可能 |
| 更新禁止項目 | `passwordHash`, トークン関連, 監査項目, `lastLoginAt`, `termsAgreedAt` は顧客・管理とも更新不可 |
| 住所管理 | `user_addresses` で複数住所を保持。顧客/管理画面の両方で CRUD |
| デフォルト住所制約 | `isDefault=true` は会員ごとに最大1件 |
| 重複メール | 管理画面の会員新規登録で重複メールは `EMAIL_ALREADY_EXISTS` で拒否 |

### 6. 返品機能

| ルール | 内容 |
|--------|------|
| 返品申請条件 | 注文ステータス = `DELIVERED` かつ配達完了日から30日以内 |
| 返品不可商品 | `products.is_returnable = false` の商品は返品申請対象外（`PRODUCT_NOT_RETURNABLE`） |
| 返品申請数量 | 注文明細ごとに0〜注文数量の範囲で指定可能 |
| 返品重複 | 同一注文に対して複数の返品申請は不可（`RETURN_ALREADY_EXISTS`） |
| 返品ステータス | `RETURN_PENDING`（申請）→ `RETURN_APPROVED`（承認）or `RETURN_CANCELLED`（拒否）→ `RETURN_CONFIRMED`（確定） |
| 返品確定後 | ステータス遷移不可（終端状態） |
| 返金・在庫戻し | 返品確定時は対象外（Phase 1） |
| 返品表示 | 顧客は注文履歴から返品状態を確認可能。管理画面で返品一覧・詳細表示、ステータスフィルタ、ページングに対応 |

---

## 在庫状態のルール（`effectiveStock` 基準）

| 在庫数 | 状態 | 表示 | バッジ色 | カート追加 |
|--------|------|------|----------|-----------|
| 6以上 | 在庫あり | 「在庫あり」 | bg-zinc-700 | 可能 |
| 1〜5 | 残りわずか | 「残りわずか」 | bg-zinc-500 | 可能 |
| 0 | 売り切れ | 「売り切れ」 | bg-zinc-400 | 不可（ボタン無効化） |

---

## 注文の状態遷移

| 状態 | 英語表記 | 説明 |
|------|----------|------|
| 作成済み | PENDING | 注文作成の初期状態 |
| 確認済み | CONFIRMED | 処理開始 |
| 出荷準備中 | PREPARING_SHIPMENT | 出荷指示生成済み |
| 発送済み | SHIPPED | 商品発送済み |
| 配達完了 | DELIVERED | 配達完了 |
| キャンセル | CANCELLED | キャンセル済み |

### 遷移ルール

| 遷移 | 条件・備考 |
|------|-----------|
| PENDING → CONFIRMED | 管理者が注文確認 |
| CONFIRMED → PREPARING_SHIPMENT | 出荷指示生成ジョブで遷移 |
| PREPARING_SHIPMENT → SHIPPED | 管理者が発送完了。`committedQuantity == orderedQuantity` が必須 |
| SHIPPED → DELIVERED | 配達完了 |
| PENDING/CONFIRMED/PREPARING_SHIPMENT → CANCELLED | 顧客or管理者がキャンセル、在庫戻し |
| SHIPPED/DELIVERED → CANCELLED | **不可** |

詳細: [注文管理](./specs/order.md)

---

## 価格・料金のルール

| 項目 | 内容 |
|------|------|
| 商品価格 | 税込、円単位、整数のみ |
| 配送料 | ¥0 固定（Phase 1） |
| 手数料 | ¥0 固定（Phase 1） |
| 合計金額 | Σ（単価×数量）+ 配送料 + 手数料 |

詳細: [商品ドメイン](./specs/product.md)

---

## セッション管理と認証

| 種別 | 保存先 | APIヘッダー | 有効期限 |
|------|--------|-----------|---------|
| セッションID（ゲスト） | localStorage(`sessionId`) | `X-Session-Id: <uuid>` | なし（永続） |
| 認証トークン（会員） | localStorage(`authToken`) | `Authorization: Bearer <token>` | 7日間 |

### データスコープ

| ユーザー種別 | カート | 注文 |
|-------------|--------|------|
| ゲスト | sessionIdでスコープ（userId=null） | sessionIdでスコープ（userId=null） |
| 会員 | userIdでスコープ | userIdでスコープ、履歴取得可能 |

ログイン時にゲストカートを会員カートにマージ。

詳細: [認証仕様](./specs/authentication.md)

---

## 今後の拡張予定

- **Phase 2**: 検索・フィルタリング、配送情報管理、決済機能
- **Phase 3**: レビュー・評価、お気に入り、再注文、AIレコメンデーション

---

## 制約事項（Phase 1）

| 制約 | 内容 |
|------|------|
| 決済機能 | 未実装（注文確定のみ） |
| 商品画像 | プレースホルダー画像（placehold.co）、アップロード不可 |
| 配送情報 | 住所登録・管理なし、配送料¥0固定 |
| 認証・認可 | 実装済み（トークンベース認証、BoUser認証） |
