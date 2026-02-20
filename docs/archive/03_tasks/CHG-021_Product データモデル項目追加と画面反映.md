# CHG-021: Product データモデル項目追加と画面反映 - 実装タスク

要件: `docs/01_requirements/CHG-021_Product データモデル項目追加と画面反映.md`
設計: `docs/02_designs/CHG-021_Product データモデル項目追加と画面反映.md`
作成日: 2026-02-20

---

## タスク一覧

### バックエンド

- [x] **T-1** `[CONTRACT]`: Flyway V10 を作成し、`products` 拡張（品番・カテゴリ参照・公開/販売期間）と `product_categories` 新設を反映する

  触る範囲: `backend/src/main/resources/db/flyway / V10__extend_product_master_and_categories.sql`

  Done: `docker compose up -d && docker compose logs backend | rg "V10__extend_product_master_and_categories|Successfully applied"` で Flyway V10 の適用を確認できること

  > 📝 ゲート高。Codex は review-note（DDL 制約・既存データ移行互換・インデックス命名の判断）を `docs/archive/04_review-note/CHG-021.md` の `## T-1` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-2** `[CONTRACT]`: Product/ProductCategory のドメイン・リポジトリ・DTO・UseCase を拡張し、公開判定と公開/販売期間整合を実装する

  触る範囲: `backend/src/main/java/com/example/aiec/modules/product / domain/entity/Product.java, domain/entity/ProductCategory.java, domain/repository/ProductRepository.java, domain/repository/ProductCategoryRepository.java, application/port/CreateProductRequest.java, application/port/UpdateProductRequest.java, application/port/ProductDto.java, application/port/ProductCategoryDto.java, application/usecase/ProductUseCase.java`

  Done: `cd backend && ./mvnw test -Dtest=ProductUseCaseTest` が通ること（`INVALID_SCHEDULE`、`CATEGORY_INACTIVE`、公開判定式を検証するケースを含むこと）

  > 📝 ゲート高。Codex は review-note（公開/販売期間の境界判定、`ITEM_NOT_FOUND` 隠蔽方針、カテゴリ公開との合成ロジック）を `docs/archive/04_review-note/CHG-021.md` の `## T-2` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-3** `[CONTRACT]`: Core API の顧客向け/管理向け商品・カテゴリエンドポイント契約を CHG-021 仕様に更新する

  触る範囲: `backend/src/main/java/com/example/aiec/modules/product/adapter/rest / ProductController.java, BoAdminProductController.java`

  Done: `cd backend && ./mvnw test -Dtest=ProductControllerContractTest,BoAdminProductControllerContractTest` が通ること

  > 📝 ゲート高。Codex は review-note（管理向け契約の互換エイリアス方針・エラーコード整合）を `docs/archive/04_review-note/CHG-021.md` の `## T-3` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-4** `[CONTRACT]`: 購入系サービスで公開商品判定（商品・カテゴリ・公開/販売期間）を適用し、非公開/期間外商品の購入を防止する

  触る範囲: `backend/src/main/java/com/example/aiec/modules/purchase/cart/service / CartService.java`

  Done: `cd backend && ./mvnw test -Dtest=CartServiceTest` が通ること（期間外・非公開カテゴリ商品の追加拒否を検証するケースを含むこと）

  > 📝 ゲート高。Codex は review-note（既存エラーメッセージ維持のための判定順序、在庫判定との優先順位）を `docs/archive/04_review-note/CHG-021.md` の `## T-4` セクションに追記し、追加検証コマンドを実行すること。

---

### BFF

- [x] **T-5** `[CONTRACT]`: Customer BFF の products API を拡張し、公開期間・販売期間を含む商品DTO正規化と返却フィルタを反映する

  触る範囲: `bff/customer-bff/src/products / products.service.ts`

  Done: `cd bff/customer-bff && npm run build` が通ること

  > 📝 ゲート高。Codex は review-note（Core API 互換フィールド吸収方針・期間判定の実装責務境界）を `docs/archive/04_review-note/CHG-021.md` の `## T-5` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-6** `[CONTRACT]`: BackOffice BFF に products モジュールを追加し、商品/カテゴリ管理 API を Core API 契約へマッピングする

  触る範囲: `bff/backoffice-bff/src/products / products.controller.ts, products.service.ts, products.module.ts`、`bff/backoffice-bff/src / app.module.ts`

  Done: `cd bff/backoffice-bff && npm run build` が通ること

  > 📝 ゲート高。Codex は review-note（`/api/admin/*` と `/api/bo/admin/*` 互換提供の判断）を `docs/archive/04_review-note/CHG-021.md` の `## T-6` セクションに追記し、追加検証コマンドを実行すること。

---

### フロントエンド

- [x] **T-7** `[SAFE]`: `entities/product` の型定義・APIクライアント・状態管理を拡張し、品番/カテゴリ/公開・販売日時を扱えるようにする

  触る範囲: `frontend/src/entities/product/model / types.ts, api.ts, ProductContext.tsx`

  Done: `npm run build --prefix frontend` が通ること

---

- [x] **T-8** `[SAFE]`: 管理画面の商品管理 UI を拡張し、商品登録/更新とカテゴリ管理で公開状態・公開/販売日時を編集できるようにする

  触る範囲: `frontend/src/pages/admin/AdminItemPage / index.tsx`

  Done: `npm run build --prefix frontend` が通ること

---

### ドキュメント

- [x] **T-9** `[CONTRACT]`: データモデル・OpenAPI・UI仕様・業務要件ドキュメントへ CHG-021 契約（`is_published` 統一、公開/販売日時、判定式）を反映する

  触る範囲: `docs/data-model.md`、`docs/specs/product.md`、`docs/ui/customer-ui.md`、`docs/ui/admin-ui.md`、`docs/requirements.md`、`docs/api/openapi.json`、`docs/api/customer-bff-openapi.json`、`docs/api/backoffice-bff-openapi.json`

  Done: `rg -n "product_code|product_categories|is_published|publish_start_at|publish_end_at|sale_start_at|sale_end_at|INVALID_SCHEDULE" docs/data-model.md docs/specs/product.md docs/ui/customer-ui.md docs/ui/admin-ui.md docs/requirements.md docs/api/openapi.json docs/api/customer-bff-openapi.json docs/api/backoffice-bff-openapi.json` で反映箇所を確認できること

  > 📝 ゲート高。Codex は review-note（契約文言の統一方針・旧 `is_active` 記述の互換扱い）を `docs/archive/04_review-note/CHG-021.md` の `## T-9` セクションに追記し、追加検証コマンドを実行すること。

---

## 実装順序

```
T-1 → T-2 → T-3 → T-4 → T-5 → T-6 → T-7 → T-8 → T-9
```

---

## 検証

### Per-task 検証コマンド

```bash
# Backend
cd backend && ./mvnw compile
cd backend && ./mvnw test -Dtest=ProductUseCaseTest
cd backend && ./mvnw test -Dtest=ProductControllerContractTest,BoAdminProductControllerContractTest
cd backend && ./mvnw test -Dtest=CartServiceTest

# BFF
cd bff/customer-bff && npm run build
cd bff/backoffice-bff && npm run build

# Frontend
npm run build --prefix frontend

# Docs
rg -n "product_code|product_categories|is_published|publish_start_at|publish_end_at|sale_start_at|sale_end_at|INVALID_SCHEDULE" \
  docs/data-model.md \
  docs/specs/product.md \
  docs/ui/customer-ui.md \
  docs/ui/admin-ui.md \
  docs/requirements.md \
  docs/api/openapi.json \
  docs/api/customer-bff-openapi.json \
  docs/api/backoffice-bff-openapi.json
```

### Final Gate（全タスク完了後に必ず実行し、結果をこのファイルに貼り付けること）

```bash
# Backend
cd backend && ./mvnw compile
cd backend && ./mvnw test

# BFF
cd bff/customer-bff && npm run build
cd bff/backoffice-bff && npm run build

# Frontend
npm run build --prefix frontend

# E2E smoke
bash -lc 'cd "$(git rev-parse --show-toplevel)/frontend" && bash ./e2e/customer-smoke.sh'
bash -lc 'cd "$(git rev-parse --show-toplevel)/frontend" && bash ./e2e/admin-smoke.sh'

# コンテナ起動確認
docker compose up -d
curl -sS -o /dev/null -w "customer-bff=%{http_code}\n" http://localhost:3001/health
curl -sS -o /dev/null -w "backoffice-bff=%{http_code}\n" http://localhost:3002/health
curl -sS -o /dev/null -w "backend=%{http_code}\n" http://localhost:8080/actuator/health
```

**Final Gate 結果:**

- `cd backend && ./mvnw compile` → PASS
- `cd backend && ./mvnw test` → PASS（91 tests, 0 failure）
- `cd bff/customer-bff && npm run build` → PASS
- `cd bff/backoffice-bff && npm run build` → PASS
- `npm run build --prefix frontend` → PASS
- `bash ./frontend/e2e/customer-smoke.sh` → PASS
- `bash ./frontend/e2e/admin-smoke.sh` → PASS
- `docker compose up -d` → PASS
- `curl http://localhost:3001/health` → `customer-bff=200`
- `curl http://localhost:3002/health` → `backoffice-bff=200`
- `curl http://localhost:8080/actuator/health` → FAIL（`backend=000`, compose 設定上 backend は host:8080 を公開していない）

## テスト手順

1. 管理画面でカテゴリを新規作成し、公開/非公開を切り替えて商品登録可否が仕様どおりであることを確認する。
2. 管理画面で商品を新規登録し、品番重複時に `PRODUCT_CODE_ALREADY_EXISTS` で拒否されることを確認する。
3. 商品の公開/販売期間を更新し、`publish_start_at > publish_end_at`、`sale_start_at > sale_end_at`、販売期間の公開期間外指定が `INVALID_SCHEDULE` で拒否されることを確認する。
4. 顧客画面で `is_published` とカテゴリ公開状態、および公開期間の組み合わせ（TT/TF/FT/FF）で表示可否が仕様どおりになることを確認する。
5. 公開期間内かつ販売期間外の商品が一覧表示はされるが購入できないことを確認する。
6. 公開期間・販売期間の境界時刻（開始時刻ちょうど/終了時刻ちょうど）で表示・購入判定が仕様どおりであることを確認する。

## Review Packet
### 変更サマリ（10行以内）
- 商品マスタを `product_code`・`category_id`・公開/販売期間へ拡張し、`product_categories` を新設した。
- Core API に管理向け商品/カテゴリ API を追加し、`/api/admin/*` と `/api/bo/admin/*` の互換を維持した。
- 顧客公開判定と購入可否判定を商品/カテゴリ公開 + 期間判定へ統一した。
- カート処理を `isPurchasable` 判定へ切り替え、購入不可商品の追加/更新拒否と自動除外を実装した。
- Customer BFF は拡張商品DTOを正規化し、公開期間フィルタを適用した。
- BackOffice BFF に products モジュールを追加し、商品/カテゴリ管理 API を中継した。
- 管理画面に商品新規登録・カテゴリ管理・公開/販売日時編集を追加した。
- データモデル/商品仕様/UI要件/OpenAPI を CHG-021 契約へ更新した。

### 変更ファイル一覧
- `backend/src/main/resources/db/flyway/V10__extend_product_master_and_categories.sql`
- `backend/src/main/java/com/example/aiec/modules/product/adapter/rest/BoAdminProductController.java`
- `backend/src/main/java/com/example/aiec/modules/product/application/port/CreateProductRequest.java`
- `backend/src/main/java/com/example/aiec/modules/product/application/port/CreateProductCategoryRequest.java`
- `backend/src/main/java/com/example/aiec/modules/product/application/port/UpdateProductCategoryRequest.java`
- `backend/src/main/java/com/example/aiec/modules/product/application/port/ProductCategoryDto.java`
- `backend/src/main/java/com/example/aiec/modules/product/application/port/ProductCommandPort.java`
- `backend/src/main/java/com/example/aiec/modules/product/application/port/ProductQueryPort.java`
- `backend/src/main/java/com/example/aiec/modules/product/application/port/ProductDto.java`
- `backend/src/main/java/com/example/aiec/modules/product/application/port/UpdateProductRequest.java`
- `backend/src/main/java/com/example/aiec/modules/product/application/usecase/ProductUseCase.java`
- `backend/src/main/java/com/example/aiec/modules/product/domain/entity/Product.java`
- `backend/src/main/java/com/example/aiec/modules/product/domain/entity/ProductCategory.java`
- `backend/src/main/java/com/example/aiec/modules/product/domain/repository/ProductRepository.java`
- `backend/src/main/java/com/example/aiec/modules/product/domain/repository/ProductCategoryRepository.java`
- `backend/src/main/java/com/example/aiec/modules/purchase/cart/service/CartService.java`
- `backend/src/main/java/com/example/aiec/config/DataLoader.java`
- `backend/src/test/java/com/example/aiec/modules/product/application/usecase/ProductUseCaseTest.java`
- `backend/src/test/java/com/example/aiec/modules/product/adapter/rest/BoAdminProductControllerContractTest.java`
- `backend/src/test/java/com/example/aiec/modules/product/adapter/rest/ProductControllerContractTest.java`
- `backend/src/test/java/com/example/aiec/modules/purchase/cart/service/CartServiceTest.java`
- `bff/customer-bff/src/products/products.service.ts`
- `bff/backoffice-bff/src/app.module.ts`
- `bff/backoffice-bff/src/products/products.module.ts`
- `bff/backoffice-bff/src/products/products.controller.ts`
- `bff/backoffice-bff/src/products/products.service.ts`
- `frontend/src/entities/product/model/types.ts`
- `frontend/src/entities/product/model/api.ts`
- `frontend/src/entities/product/model/ProductContext.tsx`
- `frontend/src/entities/product/index.ts`
- `frontend/src/pages/admin/AdminItemPage/index.tsx`
- `docs/02_designs/CHG-021_Product データモデル項目追加と画面反映.md`
- `docs/data-model.md`
- `docs/specs/product.md`
- `docs/ui/customer-ui.md`
- `docs/ui/admin-ui.md`
- `docs/requirements.md`
- `docs/api/openapi.json`
- `docs/api/customer-bff-openapi.json`
- `docs/api/backoffice-bff-openapi.json`
- `docs/archive/04_review-note/CHG-021.md`

### リスクと未解決
- Final Gate の backend health check (`http://localhost:8080/actuator/health`) は FAIL。現在の docker-compose では backend の host 公開ポートが `8000` 設定で、`8080` がホスト公開されていない。
- `backend/src/main/java/com/example/aiec/config/DataLoader.java` はタスク記載範囲外だが、V10 適用後の非NULL項目整合（初期データ投入時の失敗回避）のため追加で調整した。

### UI確認媒体（MCP/Docker）
- Docker（`frontend/e2e/customer-smoke.sh`, `frontend/e2e/admin-smoke.sh`）

### テスト結果（PASS/FAIL、失敗時は30行以内）
- [PASS] `cd backend && ./mvnw compile`
- [PASS] `cd backend && ./mvnw test`
- [PASS] `cd bff/customer-bff && npm run build`
- [PASS] `cd bff/backoffice-bff && npm run build`
- [PASS] `npm run build --prefix frontend`
- [PASS] `bash ./frontend/e2e/customer-smoke.sh`
- [PASS] `bash ./frontend/e2e/admin-smoke.sh`
- [PASS] `curl http://localhost:3001/health` (`200`)
- [PASS] `curl http://localhost:3002/health` (`200`)
- [FAIL] `curl http://localhost:8080/actuator/health` (`backend=000`, 接続不可)
