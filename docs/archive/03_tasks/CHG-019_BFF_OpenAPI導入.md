# CHG-019: BFF OpenAPI 導入（@nestjs/swagger）- 実装タスク

要件: `docs/01_requirements/CHG-019_BFF_OpenAPI導入.md`
設計: `docs/02_designs/CHG-019_BFF_OpenAPI導入.md`
作成日: 2026-02-19

---

## タスク一覧

### BFF

- [x] **T-1** `[SAFE]`: `@nestjs/swagger` パッケージを両 BFF に追加し、nest-cli.json で CLI plugin を有効化する

  触る範囲: `bff/customer-bff / package.json, nest-cli.json`、`bff/backoffice-bff / package.json, nest-cli.json`

  Done: `cd bff/customer-bff && npm install && npm run build` が通ること。backoffice-bff も同様。

---

- [x] **T-2** `[SAFE]`: `bff/shared/src/dto/` の全 DTO を `interface` から `class` に変換し `@ApiProperty()` デコレーターを付与する

  触る範囲: `bff/shared/src/dto / ProductDto, OrderDto, CartDto, UserDto, BoUserDto, InventoryDto`（および関連する型）

  Done: `cd bff/customer-bff && npm run build` および `cd bff/backoffice-bff && npm run build` が通ること。

---

- [x] **T-3** `[CONTRACT]`: Customer BFF の各コントローラーに Swagger デコレーターを付与し、`main.ts` に `SwaggerModule.setup()` を追加する

  触る範囲: `bff/customer-bff/src / main.ts`、`bff/customer-bff/src/*/*.controller.ts`（auth, products, cart, orders, members, health）

  Done: `cd bff/customer-bff && npm run build` が通ること。BFF 起動後 `curl -s -o /dev/null -w "%{http_code}" http://localhost:3001/api-docs` が `200` を返すこと。

  > 📝 ゲート高。Codex は review-note（各コントローラーへの `@ApiTags` 割り当て方針・`SWAGGER_ENABLED` ガード実装方法）を `docs/archive/04_review-note/CHG-019.md` の `## T-3` セクションに追記すること。

---

- [x] **T-4** `[CONTRACT]`: BackOffice BFF の各コントローラーに Swagger デコレーターを付与し、`main.ts` に `SwaggerModule.setup()` を追加する

  触る範囲: `bff/backoffice-bff/src / main.ts`、`bff/backoffice-bff/src/*/*.controller.ts`（bo-auth, inventory, orders, members, bo-users, health）

  Done: `cd bff/backoffice-bff && npm run build` が通ること。BFF 起動後 `curl -s -o /dev/null -w "%{http_code}" http://localhost:3002/api-docs` が `200` を返すこと。

  > 📝 ゲート高。Codex は review-note を `docs/archive/04_review-note/CHG-019.md` の `## T-4` セクションに追記すること。

---

- [x] **T-5** `[SAFE]`: Customer BFF に `generate-openapi.ts` スクリプトを作成し、`docs/api/customer-bff-openapi.json` を生成できるようにする

  触る範囲: `bff/customer-bff/src / generate-openapi.ts`、`bff/customer-bff / package.json`（`generate:openapi` スクリプト追加）

  Done: `cd bff/customer-bff && OPENAPI_GENERATE=true npm run generate:openapi` が正常終了し、`docs/api/customer-bff-openapi.json` が生成されること。

---

- [x] **T-6** `[SAFE]`: BackOffice BFF に `generate-openapi.ts` スクリプトを作成し、`docs/api/backoffice-bff-openapi.json` を生成できるようにする

  触る範囲: `bff/backoffice-bff/src / generate-openapi.ts`、`bff/backoffice-bff / package.json`（`generate:openapi` スクリプト追加）

  Done: `cd bff/backoffice-bff && OPENAPI_GENERATE=true npm run generate:openapi` が正常終了し、`docs/api/backoffice-bff-openapi.json` が生成されること。

---

- [x] **T-7** `[SAFE]`: `.github/workflows/openapi.yml` に BFF OpenAPI 生成 job を追加する

  触る範囲: `.github/workflows / openapi.yml`

  Done: `bff/**` パス変更時に `generate-bff-openapi` job がトリガーされる定義が含まれること（CI ドライラン or YAML 文法チェック `npx js-yaml .github/workflows/openapi.yml` が通ること）。

---

## 実装順序

```
T-1 → T-2 → T-3 → T-4 → T-5 → T-6 → T-7
```

T-1（パッケージ導入）→ T-2（DTO class 化）→ T-3/T-4（コントローラー注釈 + Swagger UI）→ T-5/T-6（生成スクリプト）→ T-7（CI）

T-3 と T-4 は独立しており、T-2 完了後に並行実施可能。T-5 は T-3 完了後、T-6 は T-4 完了後に実施する。

---

## 検証

### Per-task 検証コマンド

```bash
# T-1
cd bff/customer-bff && npm install && npm run build
cd bff/backoffice-bff && npm install && npm run build

# T-2, T-3, T-4
cd bff/customer-bff && npm run build
cd bff/backoffice-bff && npm run build

# T-5
cd bff/customer-bff && OPENAPI_GENERATE=true npm run generate:openapi
# → docs/api/customer-bff-openapi.json が生成されること

# T-6
cd bff/backoffice-bff && OPENAPI_GENERATE=true npm run generate:openapi
# → docs/api/backoffice-bff-openapi.json が生成されること

# T-7
npx js-yaml .github/workflows/openapi.yml
```

### Final Gate（全タスク完了後に必ず実行し、結果をこのファイルに貼り付けること）

```bash
# BFF ビルド
cd bff/customer-bff && npm run build
cd bff/backoffice-bff && npm run build

# OpenAPI spec 生成
cd bff/customer-bff && OPENAPI_GENERATE=true npm run generate:openapi
cd bff/backoffice-bff && OPENAPI_GENERATE=true npm run generate:openapi

# コンテナ起動して Swagger UI 確認
docker compose up -d
curl -s -o /dev/null -w "%{http_code}" http://localhost:3001/api-docs
curl -s -o /dev/null -w "%{http_code}" http://localhost:3002/api-docs

# SWAGGER_ENABLED=false 時の保護確認
# → 環境変数を切り替えて 404 が返ること
```

**Final Gate 結果:**
- 2026-02-19 実行
- `cd bff/customer-bff && npm run build` : PASS
- `cd bff/backoffice-bff && npm run build` : PASS
- `cd bff/customer-bff && OPENAPI_GENERATE=true npm run generate:openapi` : PASS
- `cd bff/backoffice-bff && OPENAPI_GENERATE=true npm run generate:openapi` : PASS
- `docker compose up -d` : PASS
- `curl http://localhost:3001/api-docs` : 200
- `curl http://localhost:3002/api-docs` : 200
- `SWAGGER_ENABLED=false` + 別ポート起動確認 : `http://localhost:3101/api-docs` = 404, `http://localhost:3102/api-docs` = 404

---

## テスト手順

1. Customer BFF 起動後、`http://localhost:3001/api-docs` を開き、全エンドポイントが Swagger UI に表示されること
2. BackOffice BFF 起動後、`http://localhost:3002/api-docs` を開き、全エンドポイントが Swagger UI に表示されること
3. `OPENAPI_GENERATE=true npm run generate:openapi` を実行し、生成された JSON ファイルが OpenAPI 3.x 形式であることを確認（`info.openapi` フィールドが `3.x.x` であること）
4. `SWAGGER_ENABLED=false` を設定して BFF を起動し、`/api-docs` が 404 を返すこと

## Review Packet
### 変更サマリ（10行以内）
- Customer/BackOffice BFF に `@nestjs/swagger` と CLI plugin を導入
- `bff/shared/src/dto` を `interface` から `class` へ移行し Swagger デコレーター付与
- Customer/BackOffice の `main.ts` に Swagger UI（`/api-docs`）を追加し `SWAGGER_ENABLED=false` ガードを実装
- 指定された全 controller に `@ApiTags`/`@ApiOperation`/`@ApiResponse` 系デコレーターを付与
- 両 BFF に `generate-openapi.ts` と `generate:openapi` スクリプトを追加
- `.github/workflows/openapi.yml` に BFF OpenAPI 生成ジョブを追加
- `docs/api/customer-bff-openapi.json` / `docs/api/backoffice-bff-openapi.json` を生成

### 変更ファイル一覧
- `.github/workflows/openapi.yml`
- `bff/customer-bff/package.json`
- `bff/customer-bff/nest-cli.json`
- `bff/customer-bff/src/main.ts`
- `bff/customer-bff/src/generate-openapi.ts`
- `bff/customer-bff/src/auth/auth.controller.ts`
- `bff/customer-bff/src/products/products.controller.ts`
- `bff/customer-bff/src/cart/cart.controller.ts`
- `bff/customer-bff/src/orders/orders.controller.ts`
- `bff/customer-bff/src/members/members.controller.ts`
- `bff/customer-bff/src/health/health.controller.ts`
- `bff/backoffice-bff/package.json`
- `bff/backoffice-bff/nest-cli.json`
- `bff/backoffice-bff/src/main.ts`
- `bff/backoffice-bff/src/generate-openapi.ts`
- `bff/backoffice-bff/src/auth/bo-auth.controller.ts`
- `bff/backoffice-bff/src/inventory/inventory.controller.ts`
- `bff/backoffice-bff/src/orders/orders.controller.ts`
- `bff/backoffice-bff/src/members/members.controller.ts`
- `bff/backoffice-bff/src/bo-users/bo-users.controller.ts`
- `bff/backoffice-bff/src/health/health.controller.ts`
- `bff/shared/tsconfig.json`
- `bff/shared/src/dto/product.dto.ts`
- `bff/shared/src/dto/order.dto.ts`
- `bff/shared/src/dto/cart.dto.ts`
- `bff/shared/src/dto/user.dto.ts`
- `bff/shared/src/dto/bo-user.dto.ts`
- `bff/shared/src/dto/inventory.dto.ts`
- `docs/api/customer-bff-openapi.json`
- `docs/api/backoffice-bff-openapi.json`
- `docs/archive/04_review-note/CHG-019.md`
- `docs/03_tasks/CHG-019_BFF_OpenAPI導入.md`

### リスクと未解決
- Docker build で `bff/shared` の decorator コンパイルエラーが発生したため、`bff/shared/tsconfig.json` に `experimentalDecorators`/`emitDecoratorMetadata` を追加し、DTO の必須プロパティに definite assignment (`!`) を付与して解消した。
- 既存の `docker-compose.yml` に `version` 警告が出るが、本タスク範囲外のため未対応。

### テスト結果（PASS/FAIL、失敗時は30行以内）
- [PASS] `cd bff/customer-bff && npm install && npm run build`（依存導入後にビルド成功）
- [PASS] `cd bff/backoffice-bff && npm install && npm run build`（依存導入後にビルド成功）
- [PASS] `cd bff/customer-bff && npm run build`
- [PASS] `cd bff/backoffice-bff && npm run build`
- [PASS] `cd bff/customer-bff && OPENAPI_GENERATE=true npm run generate:openapi`
- [PASS] `cd bff/backoffice-bff && OPENAPI_GENERATE=true npm run generate:openapi`
- [PASS] `npx js-yaml .github/workflows/openapi.yml`
- [PASS] `docker compose up -d` + `curl http://localhost:3001/api-docs` = 200
- [PASS] `docker compose up -d` + `curl http://localhost:3002/api-docs` = 200
- [PASS] `SWAGGER_ENABLED=false` で別ポート起動し `curl http://localhost:3101/api-docs` = 404
- [PASS] `SWAGGER_ENABLED=false` で別ポート起動し `curl http://localhost:3102/api-docs` = 404
