# CHG-021: Product データモデル項目追加と画面反映 - 技術設計

要件: `docs/01_requirements/CHG-021_Product データモデル項目追加と画面反映.md`  
作成日: 2026-02-20

**SSOT（唯一の真実）**:
- API契約: このドキュメントの「API契約」セクション
- DBスキーマ: `backend/src/main/resources/db/flyway/V10__extend_product_master_and_categories.sql`（Flywayが正）
- モジュール境界: `backend/AGENTS.md` のモジュール依存ルール
- 業務ルール: `docs/requirements.md`（公開判定・在庫判定・価格ルール）

---

## 1. 設計方針

### 1.1 スコープと責務

- 商品の販売実体は現行どおり `products` を維持する（在庫・価格・公開/販売期間は商品単位）。
- 商品マスタ項目を拡張し、品番（`productCode`）とカテゴリ参照（`categoryId`）を追加する。
- 商品カテゴリは `product_categories` でマスタ管理する。
- 商品画像は現行どおり `products.image` を継続利用する。
- 管理画面の商品運用は「商品登録（新規作成） + 商品更新 + カテゴリ管理」を提供する。
- 顧客向け公開判定は「商品公開」と「カテゴリ公開」の両方を満たすことを条件とする。

### 1.2 データモデル契約

`products` の拡張項目:
- `product_code`（品番、必須、一意）
- `category_id`（カテゴリマスタ参照、必須）
- `publish_start_at` / `publish_end_at`（公開期間、任意）
- `sale_start_at` / `sale_end_at`（販売期間、任意）

`product_categories` の管理項目:
- `id`, `name`, `display_order`, `is_published`
- 監査カラム + 論理削除（既存主要テーブルに準拠）

整合ルール:
- 商品は必ず1つのカテゴリに所属する。
- 品番は重複不可。
- 商品価格は 0 以上の整数（円）を許容する。
- `is_published=false` のカテゴリは商品登録/更新時に指定不可。
- 公開期間は `publish_start_at <= publish_end_at` を満たすこと（両方指定時）。
- 販売期間は `sale_start_at <= sale_end_at` を満たすこと（両方指定時）。
- `sale_start_at`/`sale_end_at` を指定する場合、公開期間内であること。
- 既存商品データは移行時にカテゴリと品番を補完し、整合状態を維持する。

### 1.3 既存ルールの維持

- 公開制御フラグは商品・カテゴリともに `is_published` を採用する。
- 商品の「利用可否」を表す `is_active` は本スコープでは扱わず、商品には導入しない。
- 顧客向け表示は `product.is_published && category.is_published && 公開期間内(now ∈ [publish_start_at, publish_end_at])` を満たす商品のみとする。
- 購入可能判定は `顧客向け表示可能 && 販売期間内(now ∈ [sale_start_at, sale_end_at]) && stock > 0` とする。
- 在庫状態判定（6以上/1〜5/0）は現行ルールを維持。
- 商品価格は税込・円単位・整数運用（型は既存 BigDecimal 運用を維持）。
- 日時は API 入出力を ISO 8601 とし、保存は `TIMESTAMP WITH TIME ZONE` に統一する。
- 注文/在庫引当/カートの識別子は `product_id` のまま変更しない。

---

## 2. API契約

### 2.1 Core API契約

#### 顧客向け（既存拡張）

| エンドポイント | メソッド | 契約 |
|---|---|---|
| `/api/item` | GET | 公開商品の一覧を返却（`ProductDto` 拡張項目を含む） |
| `/api/item/{id}` | GET | 公開商品の詳細を返却（`ProductDto` 拡張項目を含む） |

`ProductDto`（顧客向け公開項目）:
- `id`, `productCode`, `name`, `description`, `categoryId`, `categoryName`, `price`, `stock`, `image`, `isPublished`, `publishStartAt`, `publishEndAt`, `saleStartAt`, `saleEndAt`

#### 管理向け（新規）

| エンドポイント | メソッド | 認証/認可 | 契約 |
|---|---|---|---|
| `/api/bo/admin/items` | GET | BoUser | 商品一覧（公開/非公開を含む） |
| `/api/bo/admin/items/{id}` | GET | BoUser | 商品詳細（公開状態に関係なく取得可能） |
| `/api/bo/admin/items` | POST | BoUser(ADMIN以上) | 商品新規登録 |
| `/api/bo/admin/items/{id}` | PUT | BoUser(ADMIN以上) | 商品更新 |
| `/api/bo/admin/item-categories` | GET | BoUser | カテゴリ一覧取得 |
| `/api/bo/admin/item-categories` | POST | BoUser(ADMIN以上) | カテゴリ新規登録 |
| `/api/bo/admin/item-categories/{id}` | PUT | BoUser(ADMIN以上) | カテゴリ更新（名称/表示順/公開状態） |

`CreateProductRequest`:
- `productCode`, `name`, `description`, `categoryId`, `price`, `stock`, `isPublished`, `publishStartAt`, `publishEndAt`, `saleStartAt`, `saleEndAt`
- `image` は任意（未指定時は現行プレースホルダー運用に整合する値を適用）

`UpdateProductRequest`:
- `name`, `description`, `categoryId`, `price`, `stock`, `isPublished`, `publishStartAt`, `publishEndAt`, `saleStartAt`, `saleEndAt`, `image`

`ProductCategoryDto`:
- `id`, `name`, `displayOrder`, `isPublished`

エラー契約:
- `ITEM_NOT_FOUND`（404）
- `CATEGORY_NOT_FOUND`（404）
- `CATEGORY_INACTIVE`（400, カテゴリが非公開）
- `INVALID_SCHEDULE`（400, 公開期間/販売期間の整合エラー）
- `PRODUCT_CODE_ALREADY_EXISTS`（409）
- `CATEGORY_ALREADY_EXISTS`（409）
- `INVALID_REQUEST`（400）

### 2.2 Customer BFF契約

- 既存の `/api/products`, `/api/products/{id}`, `/api/products/{id}/full` を維持する。
- Core API `ProductDto` の拡張項目（`productCode`, `categoryId`, `categoryName`）を透過・正規化して返却する。
- 顧客向け返却対象は `product.is_published && category.is_published && 公開期間内(now ∈ [publish_start_at, publish_end_at])` を満たす商品に限定する。
- キャッシュキー設計（商品一覧/商品詳細）は現行方針を維持する。

### 2.3 BackOffice BFF契約

新規に商品管理モジュールを追加し、以下を提供する。

| エンドポイント | メソッド | 契約 |
|---|---|---|
| `/api/admin/items` | GET | 商品一覧取得（管理向け） |
| `/api/admin/items/{id}` | GET | 商品詳細取得（管理向け） |
| `/api/admin/items` | POST | 商品新規登録 |
| `/api/admin/items/{id}` | PUT | 商品更新 |
| `/api/admin/item-categories` | GET | カテゴリ一覧取得 |
| `/api/admin/item-categories` | POST | カテゴリ新規登録 |
| `/api/admin/item-categories/{id}` | PUT | カテゴリ更新 |

互換エイリアス:
- 既存管理画面との互換のため、`/api/bo/admin/items/*` を同等契約として提供する。

---

## 3. モジュール・レイヤ構成

### 3.1 Backend（Core API）

```
product/
  domain/entity/
    Product                    (拡張: productCode/category参照)
    ProductCategory            (新規)
  domain/repository/
    ProductRepository          (拡張)
    ProductCategoryRepository  (新規)
  application/port/
    ProductDto                 (拡張)
    UpdateProductRequest       (拡張)
    CreateProductRequest       (新規)
    ProductCategoryDto         (新規)
    CreateProductCategoryRequest (新規)
    UpdateProductCategoryRequest (新規)
  application/usecase/
    ProductUseCase             (拡張: 管理向け登録/更新/カテゴリ管理)
  adapter/rest/
    ProductController          (顧客向け取得契約)
    BoAdminProductController   (新規: 管理向け商品/カテゴリ契約)
```

### 3.2 BFF

```
customer-bff/
  products/
    products.service.ts        (拡張: ProductDTO拡張項目マッピング)

backoffice-bff/
  products/                    (新規)
    products.controller.ts
    products.service.ts
    products.module.ts
  app.module.ts                (products module 追加)
```

### 3.3 Frontend

```
entities/product/
  model/types.ts               (Product型拡張、CreateProductRequest追加)
  model/api.ts                 (商品登録・カテゴリ取得API追加)
  model/ProductContext.tsx     (登録/更新後の状態反映拡張)

pages/admin/
  AdminItemPage/index.tsx      (商品登録UI + カテゴリ選択 + 拡張項目編集)
```

---

## 4. 主要クラス/IFの責務

| クラス/IF | 責務 | レイヤ |
|---|---|---|
| `Product` | 商品マスタ（品番・カテゴリ参照を含む販売実体） | product/domain |
| `ProductCategory` | 商品カテゴリマスタ | product/domain |
| `ProductUseCase` | 商品取得/登録/更新、およびカテゴリ取得/登録/更新のユースケース | product/application |
| `ProductController` | 顧客向け商品取得API境界 | product/adapter |
| `BoAdminProductController` | 管理向け商品・カテゴリAPI境界（認証/認可） | product/adapter |
| `ProductsService` (customer-bff) | 顧客向け商品レスポンス整形・キャッシュ | bff/customer |
| `ProductsService` (backoffice-bff) | 管理向け商品/カテゴリAPIのCore中継 | bff/backoffice |
| `AdminItemPage` | 商品登録・商品編集・カテゴリ選択のUI責務 | frontend/pages |

---

## 5. トランザクション・非同期方針

- 商品登録（POST）と商品更新（PUT）は単一トランザクションで処理する。
- カテゴリ登録/更新もAPI単位で単一トランザクションとする。
- 商品更新時のカテゴリ整合（存在・公開状態）と公開/販売期間整合は同一トランザクション内で検証する。
- 管理操作の監査は既存の `OPERATION_PERFORMED` outbox 発行方針を継続する。
- 新規の非同期処理・リトライ戦略は追加しない。

---

## 6. 処理フロー

### 6.1 管理画面での商品登録

```
AdminItemPage
  → BackOffice BFF /api/admin/items (POST)
    → Core API /api/bo/admin/items (POST)
      → ProductUseCase
        → category検証
        → products 登録
      → ProductDto 返却
  → 一覧再取得/画面反映
```

### 6.2 顧客画面での商品参照

```
Customer UI
  → Customer BFF /api/products
    → Core API /api/item
      → 商品公開 AND カテゴリ公開 AND 公開期間内の商品のみ取得
      → ProductDto（拡張項目含む）返却
  → 商品一覧描画
```

---

## 7. 影響範囲

| 区分 | 対象（クラス/ファイル） | 変更概要 |
|---|---|---|
| 新規作成 | `backend/src/main/resources/db/flyway/V10__extend_product_master_and_categories.sql` | 商品マスタ拡張 + カテゴリマスタ新設 |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/product/domain/entity/Product.java` | 品番・カテゴリ参照・公開/販売日時の追加 |
| 新規作成 | `backend/src/main/java/com/example/aiec/modules/product/domain/entity/ProductCategory.java` | カテゴリエンティティ |
| 新規作成 | `backend/src/main/java/com/example/aiec/modules/product/domain/repository/ProductCategoryRepository.java` | カテゴリリポジトリ |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/product/domain/repository/ProductRepository.java` | 管理向け検索/重複検証クエリ拡張 |
| 新規作成 | `backend/src/main/java/com/example/aiec/modules/product/application/port/CreateProductRequest.java` | 商品登録リクエスト |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/product/application/port/UpdateProductRequest.java` | 更新可能項目（公開/販売日時含む）拡張 |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/product/application/port/ProductDto.java` | 拡張項目（公開/販売日時含む）反映 |
| 新規作成 | `backend/src/main/java/com/example/aiec/modules/product/application/port/ProductCategoryDto.java` | カテゴリDTO |
| 新規作成 | `backend/src/main/java/com/example/aiec/modules/product/adapter/rest/BoAdminProductController.java` | 管理向け商品/カテゴリAPI |
| 既存変更 | `backend/src/main/java/com/example/aiec/modules/product/application/usecase/ProductUseCase.java` | 商品登録・カテゴリ管理ユースケース追加 |
| 既存変更 | `bff/customer-bff/src/products/products.service.ts` | 拡張商品項目の正規化反映 |
| 新規作成 | `bff/backoffice-bff/src/products/products.controller.ts` | 管理向け商品/カテゴリBFF API |
| 新規作成 | `bff/backoffice-bff/src/products/products.service.ts` | Core API中継 + キャッシュ無効化 |
| 新規作成 | `bff/backoffice-bff/src/products/products.module.ts` | モジュール定義 |
| 既存変更 | `bff/backoffice-bff/src/app.module.ts` | products module 組み込み |
| 既存変更 | `frontend/src/entities/product/model/types.ts` | Product型/登録型拡張 |
| 既存変更 | `frontend/src/entities/product/model/api.ts` | 商品登録・カテゴリAPI追加 |
| 既存変更 | `frontend/src/entities/product/model/ProductContext.tsx` | 登録・更新後の状態管理拡張 |
| 既存変更 | `frontend/src/pages/admin/AdminItemPage/index.tsx` | 商品登録UIとカテゴリ編集導線 |
| 既存変更 | `docs/data-model.md` | products拡張 + product_categories反映 |
| 既存変更 | `docs/specs/product.md` | 品番/カテゴリ/商品登録ルール反映 |
| 既存変更 | `docs/ui/admin-ui.md` | 商品登録要件・カテゴリ運用反映 |
| 既存変更 | `docs/ui/customer-ui.md` | 拡張商品項目の表示要件反映 |
| 既存変更 | `docs/requirements.md` | 商品マスタ拡張・カテゴリ管理要件反映 |
| 既存変更 | `docs/api/openapi.json` | Core API契約更新 |
| 既存変更 | `docs/api/customer-bff-openapi.json` | Customer BFF契約更新 |
| 既存変更 | `docs/api/backoffice-bff-openapi.json` | BackOffice BFF契約更新 |

---

## 8. テスト観点

- 正常系:
  - 管理画面から商品新規登録ができる。
  - 商品更新でカテゴリ・価格・在庫・公開状態・公開/販売日時を更新できる。
  - カテゴリマスタの追加/更新ができる。
  - 顧客画面で `product.is_published=true` かつ `category.is_published=true` かつ公開期間内の商品が表示される。
- 異常系:
  - 重複品番で登録すると `PRODUCT_CODE_ALREADY_EXISTS` になる。
  - 存在しないカテゴリ指定で登録/更新すると `CATEGORY_NOT_FOUND` になる。
  - 非公開カテゴリ指定で登録/更新すると `CATEGORY_INACTIVE` になる。
  - `publish_start_at > publish_end_at` または `sale_start_at > sale_end_at` は `INVALID_SCHEDULE` になる。
  - 販売期間が公開期間外になる入力は `INVALID_SCHEDULE` になる。
  - `product.is_published=true` かつ `category.is_published=false` の商品は顧客画面に表示されない。
  - `product.is_published=false` かつ `category.is_published=true` の商品は顧客画面に表示されない。
- 不正入力（価格<0、在庫<0など）で `INVALID_REQUEST` になる。
- 境界値:
  - 既存商品データの移行後、商品一覧/商品詳細が継続利用できる。
  - カテゴリ0件（理論上）を避ける初期マスタ投入後に商品登録可能である。
  - 公開判定4パターン（TT/TF/FT/FF）で表示可否が仕様どおりとなる。
  - 公開開始/終了、販売開始/終了の境界時刻（ちょうど開始時刻・終了時刻）で判定が正しい。
  - 在庫状態の閾値（0,1,5,6）表示が既存ルールどおり維持される。
