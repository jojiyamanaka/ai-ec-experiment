# CHG-019: BFF OpenAPI 導入（@nestjs/swagger）- 技術設計

要件: `docs/01_requirements/CHG-019_BFF_OpenAPI導入.md`
作成日: 2026-02-19

**SSOT（唯一の真実）**:
- API契約: `docs/api/customer-bff-openapi.json` / `docs/api/backoffice-bff-openapi.json`（生成物が正）
- エンドポイント定義: 各 BFF の Controller ソースコード（Swagger 注釈が正）
- DTO 型定義: `bff/shared/src/dto/`（クラス定義が正）

---

## 1. 設計方針

### 基本方針

Core API が springdoc-openapi でコード駆動の OpenAPI spec を生成・管理しているパターンに倣い、BFF も `@nestjs/swagger` を使ってコードから spec を自動生成する。設計書手書きの API 仕様（`docs/ui/api-spec.md`）は今後 BFF spec JSON に取って代わられる。

### DTO の class 化

現状 `bff/shared/src/dto/` の型定義は TypeScript `interface` であり、`@nestjs/swagger` の `@ApiProperty()` デコレーターを付与できない。**class ベースへ移行**する。フロントエンドが直接インポートする型定義ではないため（フロントエンドは `@shared/types` を使用）、影響範囲は BFF 内のみ。

### spec 生成方法

NestJS アプリを「ヘッドレス起動（HTTP ポートを bind しない）」して `SwaggerModule.createDocument()` で document object を生成し JSON ファイルへ書き出す独立スクリプト `generate-openapi.ts` を各 BFF に置く。実際のサーバーを起動せずに DB・Redis 等外部依存なしで動作させるため、`NestFactory.create()` 時に外部接続が必要なモジュール（Redis等）を環境変数で無効化可能にする。

### Swagger UI

ローカル開発時の利便性のため、`/api-docs` で Swagger UI を提供する。production 環境では環境変数 `SWAGGER_ENABLED=false` で無効化できる。

### CI 統合

既存の `.github/workflows/openapi.yml` に BFF の job を追加する（Core API との分離は不要。同一ワークフローで管理が容易）。

---

## 2. 影響範囲

| 区分 | 対象 | 変更概要 |
|------|------|---------|
| 既存変更 | `bff/customer-bff/package.json` | `@nestjs/swagger`, `swagger-ui-express` を dependencies に追加 |
| 既存変更 | `bff/backoffice-bff/package.json` | 同上 |
| 既存変更 | `bff/customer-bff/nest-cli.json` | `@nestjs/swagger` CLI plugin を有効化（`@ApiProperty()` 省略オプション） |
| 既存変更 | `bff/backoffice-bff/nest-cli.json` | 同上 |
| 既存変更 | `bff/shared/src/dto/*.ts` | `interface` → `class` に変換、`@ApiProperty()` デコレーター付与 |
| 既存変更 | `bff/customer-bff/src/main.ts` | `SwaggerModule.setup()` 追加（`SWAGGER_ENABLED` ガード付き） |
| 既存変更 | `bff/backoffice-bff/src/main.ts` | 同上 |
| 既存変更 | `bff/customer-bff/src/**/*.controller.ts` | `@ApiTags()`, `@ApiOperation()`, `@ApiResponse()` デコレーター付与 |
| 既存変更 | `bff/backoffice-bff/src/**/*.controller.ts` | 同上 |
| 新規作成 | `bff/customer-bff/src/generate-openapi.ts` | spec 生成スクリプト |
| 新規作成 | `bff/backoffice-bff/src/generate-openapi.ts` | spec 生成スクリプト |
| 既存変更 | `.github/workflows/openapi.yml` | BFF 用 job を追加 |
| 新規作成 | `docs/api/customer-bff-openapi.json` | 生成物（git 管理） |
| 新規作成 | `docs/api/backoffice-bff-openapi.json` | 生成物（git 管理） |

---

## 3. SwaggerModule 設定

### Customer BFF（`/api-docs`）

```ts
// SwaggerDocumentBuilder の設定値（契約）
title: 'Customer BFF API'
description: '顧客向け BFF エンドポイント'
version: '1.0'
path: 'api-docs'
```

### BackOffice BFF（`/api-docs`）

```ts
title: 'BackOffice BFF API'
description: '管理向け BFF エンドポイント'
version: '1.0'
path: 'api-docs'
```

---

## 4. 生成スクリプト設計

`generate-openapi.ts` のシグネチャ（契約）:

- 引数なしで実行可能（`npx ts-node src/generate-openapi.ts`）
- 出力先: プロジェクトルート相対で `../../docs/api/{bff-name}-openapi.json`
- 終了後 `app.close()` を呼び、プロセスを正常終了させる
- 外部依存（Redis, Core API）への接続を行わないため、環境変数 `OPENAPI_GENERATE=true` を設定した際に Redis モジュールの接続確立をスキップする

`package.json` への script 追加（契約）:

```json
"generate:openapi": "ts-node -r tsconfig-paths/register src/generate-openapi.ts"
```

---

## 5. DTO class 移行方針

### 移行対象（`bff/shared/src/dto/`）

| ファイル | 変更 |
|---------|------|
| `product.dto.ts` | `interface ProductDto` → `export class ProductDto` |
| `order.dto.ts` | 同上 |
| `cart.dto.ts` | 同上 |
| `user.dto.ts` | 同上 |
| `bo-user.dto.ts` | 同上 |
| `inventory.dto.ts` | 同上 |

### `@ApiProperty()` 付与方針

- CLI plugin を使用するため、プリミティブ型（`string`, `number`, `boolean`）は自動推論される（デコレーター省略可）
- オプショナルフィールド（`?`）は `@ApiPropertyOptional()` を付与
- 配列・ネスト型は明示的に `@ApiProperty({ type: () => XxxDto, isArray: true })` を付与

---

## 6. CI 設計（`.github/workflows/openapi.yml` 拡張）

既存 workflow に `generate-bff-openapi` job を追加する。

### トリガー条件（追加）

```yaml
paths:
  - 'bff/**'
```

### job の処理フロー

```
checkout → setup Node 20 → npm ci (customer-bff) → generate:openapi
                         → npm ci (backoffice-bff) → generate:openapi
→ git add docs/api/*-bff-openapi.json → commit & push（main のみ）
```

### 注意点

- `generate:openapi` 実行時は Redis・Core API への接続が発生しないよう `OPENAPI_GENERATE=true` を環境変数としてセットする
- commit は Core API 同様 `github-actions[bot]` ユーザーで行う

---

## 7. 処理フロー（ローカル開発）

```
開発者がコントローラーを変更
  → npm run generate:openapi
  → docs/api/{bff-name}-openapi.json が更新される
  → git commit / PR

BFF 起動中
  → http://localhost:3001/api-docs にアクセス
  → Swagger UI でエンドポイント一覧・試打が可能
```

---

## 8. テスト観点

- Swagger UI（`/api-docs`）が BFF 起動時に 200 を返すこと
- `generate:openapi` スクリプトが DB・Redis なしで（`OPENAPI_GENERATE=true` 環境下で）正常終了し、JSON ファイルが出力されること
- 生成された JSON が valid な OpenAPI 3.x 形式であること（CI で `openapi-validator` 等による検証を任意追加）
- `SWAGGER_ENABLED=false` のとき `/api-docs` が 404 を返すこと（production 保護）
