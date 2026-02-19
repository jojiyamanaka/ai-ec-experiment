# BFF アーキテクチャ仕様書

作成日: 2026-02-17
バージョン: 1.1

## 概要

AI EC Experiment の BFF（Backend for Frontend）層のアーキテクチャを定義する。
BFF は顧客向け（Customer BFF）と管理向け（BackOffice BFF）の2つに分離されており、
フロントエンドと Core API の間でリクエストのプロキシ、認証、エラーハンドリングを担当する。

**関連ドキュメント**:
- [技術仕様](../SPEC.md) - 全体アーキテクチャ
- [API仕様](../ui/api-spec.md) - エンドポイント詳細・レスポンス型
- [認証仕様](./authentication.md) - 認証・認可

---

## 1. 設計原則

- **ドメイン分離**: 顧客向けと管理向けの認証・認可を完全に分離
- **キャッシュ層**: Redis を使ったレスポンスキャッシュ・セッション管理・レート制限
- **レスポンス集約**: 複数 Core API 呼び出しを1リクエストにまとめる集約エンドポイント
- **認証境界**: 顧客トークンと管理者トークンの境界を BFF 層で強制
- **エラー変換**: Core API のエラーレスポンスをフロントエンド向けに統一フォーマットで返す

---

## 2. Customer BFF（顧客向け）

**ベースURL**: `http://localhost:3001`
**フレームワーク**: NestJS
**Core API 接続先**: `http://backend:8080`（Docker 内部ネットワーク）

### エンドポイント一覧

| BFF エンドポイント | メソッド | 説明 | 認証 | Core API マッピング |
|---|---|---|---|---|
| /health | GET | ヘルスチェック | 不要 | — |
| /api/products | GET | 商品一覧取得 | 不要 | GET /api/item |（キャッシュTTL 3分）|
| /api/products/:id | GET | 商品詳細取得 | 不要 | GET /api/item/:id |（キャッシュTTL 10分）|
| /api/products/:id/full | GET | 商品詳細+関連商品集約 | 不要 | 並列: GET /api/item/:id + GET /api/item |
| /api/cart | GET | カート取得 | User | GET /api/order/cart |
| /api/cart/items | POST | カート追加 | User | POST /api/order/cart/items |
| /api/cart/items/:id | PUT | カート数量変更 | User | PUT /api/order/cart/items/:id |
| /api/cart/items/:id | DELETE | カート商品削除 | User | DELETE /api/order/cart/items/:id |
| /api/auth/register | POST | 会員登録 | 不要 | POST /api/auth/register |
| /api/auth/login | POST | 会員ログイン | 不要 | POST /api/auth/login |
| /api/auth/logout | POST | 会員ログアウト | User | POST /api/auth/logout |
| /api/members/me | GET | 会員情報取得 | User | GET /api/auth/me |
| /api/orders | POST | 注文確定 | User | POST /api/order |
| /api/orders | GET | 注文一覧（会員） | User | GET /api/order |
| /api/orders/history | GET | 注文履歴 | User | GET /api/order/history |
| /api/orders/:id | GET | 注文詳細 | User | GET /api/order/:id |
| /api/orders/:id/full | GET | 注文詳細+商品情報集約 | User | 並列: GET /api/order/:id + 各商品取得 |
| /api/orders/:id/cancel | POST | 注文キャンセル | User | POST /api/order/:id/cancel |

### 認証方式

- **認証ヘッダー**: `Authorization: Bearer <token>`
- **トークン検証**: `auth.guard.ts` — Redis キャッシュ確認 → MISS 時 Core API `/api/auth/me` 呼び出し（TTL 1分）
- **セッション管理**: `X-Session-Id` ヘッダーで識別。初回アクセス時 BFF が UUID 生成、Redis に保存（アイドル30分）

### レート制限

Redis INCR+EXPIRE 方式。制限超過時は 429 + `X-RateLimit-*` ヘッダーを返す。

| エンドポイント | 制限単位 | 制限値 |
|---|---|---|
| 全 API | IP | 100req/分 |
| GET /api/products | IP | 20req/分 |
| POST /api/cart/* | User | 10req/分 |
| POST /api/auth/login | IP | 5req/分 |
| POST /api/auth/register | IP | 3req/10分 |

### モジュール構成

```
customer-bff/src/
├── main.ts, app.module.ts
├── config/configuration.ts
├── auth/          # POST /api/auth/register, login, logout + auth.guard.ts
├── products/      # GET /api/products, /api/products/:id, /api/products/:id/full
├── cart/          # GET/POST/PUT/DELETE /api/cart/**
├── orders/        # GET/POST /api/orders/**, /api/orders/:id/full
├── members/       # GET /api/members/me
├── session/       # セッション管理（Redis）
├── redis/         # Redis クライアント・モジュール
├── core-api/      # Core API HTTP クライアント（共通）
└── common/        # filters/, interceptors/, guards/ (rate-limit)
```

---

## 3. BackOffice BFF（管理向け）

**ベースURL**: `http://localhost:3002`
**フレームワーク**: NestJS
**Core API 接続先**: `http://backend:8080`（Docker 内部ネットワーク）

### エンドポイント一覧

| BFF エンドポイント | メソッド | 説明 | 認証 |
|---|---|---|---|
| /health | GET | ヘルスチェック | 不要 |
| /api/bo-auth/login | POST | 管理ログイン | 不要 |
| /api/bo-auth/logout | POST | 管理ログアウト | BoUser |
| /api/inventory | GET | 在庫一覧取得 | BoUser |
| /api/inventory/adjustments | GET | 在庫調整履歴 | BoUser |
| /api/inventory/adjust | POST | 在庫調整 | BoUser |
| /api/inventory/:id | PUT | 在庫更新 | BoUser |
| /api/admin/orders | GET | 注文一覧取得 | BoUser |
| /api/admin/orders/:id | GET | 注文詳細取得 | BoUser |
| /api/admin/orders/:id | PUT | 注文更新 | BoUser |
| /api/admin/orders/:id/confirm | POST | 注文確認 | BoUser |
| /api/admin/orders/:id/ship | POST | 注文発送 | BoUser |
| /api/admin/orders/:id/deliver | POST | 配達完了 | BoUser |
| /api/admin/orders/:id/cancel | POST | 注文キャンセル | BoUser |
| /api/order/* | * | 互換エイリアス（上記の /api/admin/orders と同等） | BoUser |
| /api/admin/members | GET | 会員一覧取得 | BoUser |
| /api/admin/members/:id | GET | 会員詳細取得 | BoUser |
| /api/admin/members/:id/status | PUT | 会員状態更新 | BoUser |
| /api/admin/bo-users | GET | BoUser一覧取得 | BoUser |
| /api/admin/bo-users | POST | BoUser作成 | BoUser |

### 認証方式

- **認証ヘッダー**: `Authorization: Bearer <bo_token>`
- **トークン検証**: `bo-auth.guard.ts` — Redis キャッシュ確認 → MISS 時 Core API `/api/bo-auth/me` 呼び出し（Redis Key: `bo-auth:token:{tokenHash}`, TTL 60秒）
- **顧客トークン拒否**: 顧客トークンで管理APIにアクセスした場合は `CUSTOMER_TOKEN_NOT_ALLOWED` (403) を返す
- **キャッシュ制御**: 全レスポンスに `Cache-Control: no-store` を付与

### モジュール構成

```
backoffice-bff/src/
├── main.ts, app.module.ts
├── config/configuration.ts
├── auth/          # POST /api/bo-auth/login, logout + bo-auth.guard.ts
├── orders/        # GET/POST/PUT /api/admin/orders/**
├── inventory/     # GET/POST/PUT /api/inventory/**
├── members/       # GET/PUT /api/admin/members/**
├── bo-users/      # GET/POST /api/admin/bo-users/**
├── core-api/      # Core API HTTP クライアント（共通）
└── common/        # filters/, interceptors/ (logging, trace)
```

---

## 4. Core API クライアント

両 BFF に共通の `CoreApiService` が Core API への HTTP 通信を担当する。

**責務**: Core API への HTTP リクエスト送信、ヘッダー転送（`Authorization`, `X-Session-Id`）、エラーレスポンスの変換、接続タイムアウトの管理

**エラーハンドリング**: Core API からのエラーレスポンスは `HttpExceptionFilter` で統一フォーマットに変換:
`{ "success": false, "error": { "code": "ERROR_CODE", "message": "エラーメッセージ" } }`

---

## 5. 共通機能

- **ログインターセプター**: 全リクエストのメソッド・パス・ステータスコード・処理時間を記録。OTel API から traceId/spanId を取得してログに付与（OTel未初期化時は空文字）
- **トレースインターセプター**: OTel SDK が `traceparent` ヘッダを自動処理。trace_id を `request.traceId` に転写し `X-Trace-Id` ヘッダとして返却（UUID生成は廃止）
- **OTel SDK 初期化**: `tracing.ts` を `--require ./dist/tracing` で main.ts より先にロード。OTLP/gRPC でコレクターへエクスポート

---

## 6. Core API（内部専用）

**ベースURL**: `http://backend:8080`（内部ネットワークのみ）
- 外部からの直接アクセスは不可
- BFF経由でのみアクセス可能

---

## 7. OpenAPI スペック

### 自動生成・契約管理

- **Customer BFF**: `docs/api/customer-bff-openapi.json`（SSOT、開発ガイド用）
- **BackOffice BFF**: `docs/api/backoffice-bff-openapi.json`（SSOT、開発ガイド用）

ビルド時にコントローラー・DTO の `@nestjs/swagger` デコレーターから自動生成。各エンドポイント仕様の正式契約は JSON ファイルである。

### Swagger UI

ローカル開発時、BFF 起動後:
- Customer BFF: `http://localhost:3001/api-docs`
- BackOffice BFF: `http://localhost:3002/api-docs`

エンドポイント試打（Try it out）が可能。Production では環境変数 `SWAGGER_ENABLED=false` で無効化。

### ローカル開発フロー

```
1. コントローラー/DTO を変更
2. npm run generate:openapi
3. docs/api/{bff-name}-openapi.json が更新される
4. git add/commit で追跡
```

---

## 関連資料

- **技術仕様**: [SPEC.md](../SPEC.md)
- **API仕様**: [api-spec.md](../ui/api-spec.md)
- **認証仕様**: [authentication.md](./authentication.md)
