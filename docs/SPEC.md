# AI EC Experiment - 技術仕様書

## プロジェクト概要

AIがおすすめする商品を販売するECサイトのプロトタイプ。

**目的**: AI推薦機能を備えたECサイトの技術検証とプロトタイピング

**技術スタック**:
- **フロントエンド**: React 19 + TypeScript + Vite
- **スタイリング**: Tailwind CSS 4
- **ルーティング**: React Router v7
- **BFF**: NestJS（Customer BFF / BackOffice BFF）
- **バックエンド**: Spring Boot 3.4.2 + Java 21
- **データベース**: PostgreSQL + Hibernate
- **コンテナ**: Docker + Docker Compose

---

## 技術アーキテクチャ

```
┌──────────────────┐     ┌──────────────────┐
│ Frontend(Customer)│     │ Frontend(Admin)  │
│  (React, 5173)    │     │  (React, 5174)   │
└────────┬──────────┘     └────────┬─────────┘
         │ HTTP                           │ HTTP
         ▼                                ▼
┌──────────────────┐     ┌──────────────────┐
│ Customer BFF     │     │ BackOffice BFF   │
│ (NestJS, 3001)   │     │ (NestJS, 3002)   │
└────────┬─────────┘     └────────┬─────────┘
         │ internal HTTP                 │ internal HTTP
         └──────────────┬────────────────┘
                        ▼
               ┌──────────────────┐
               │ Core API         │
               │ (Spring, 8080)   │  ※ internal network only
               └────────┬─────────┘
                        │ JDBC
                        ▼
               ┌──────────────────┐
               │ PostgreSQL 16    │
               └──────────────────┘
```

---

## 関連ドキュメント

このファイルはアーキテクチャ概要のみを記載します。詳細は以下を参照してください。

### 基本仕様
- **[業務要件・ビジネスルール](./requirements.md)** - 主要機能、在庫状態のルール、注文の状態遷移
- **[データモデル](./data-model.md)** - エンティティ定義、データベーススキーマ、型定義
- **[デザインシステム](./design-system.md)** - UI設計方針、コンポーネント規約

### UI層
- **[顧客向け画面](./ui/customer-ui.md)** - 画面一覧、画面遷移図、UI/UX設計思想
- **[管理画面](./ui/admin-ui.md)** - 商品管理・注文管理のUI仕様
- **[API仕様](./ui/api-spec.md)** - APIエンドポイント、リクエスト/レスポンス仕様（手書き参照用）
- **[Core API OpenAPI仕様](./api/openapi.json)** - springdoc-openapi 自動生成の機械可読仕様（正式契約）

### ドメイン層
- **[商品ドメイン](./specs/product.md)** - 商品マスタ、価格管理、公開制御
- **[在庫ドメイン](./specs/inventory.md)** - 在庫引当（仮引当・本引当）の詳細仕様
- **[注文ドメイン](./specs/order.md)** - 注文フロー、状態遷移の詳細仕様
- **[認証](./specs/authentication.md)** - 顧客/管理者の認証・認可
- **[BFFアーキテクチャ](./specs/bff-architecture.md)** - Customer BFF / BackOffice BFF の構成

### タスク別ドキュメント参照ガイド

| 作業内容 | 参照すべきドキュメント |
|---------|---------------------|
| フロントエンド（顧客画面） | customer-ui.md, api-spec.md |
| フロントエンド（管理画面） | admin-ui.md, api-spec.md |
| バックエンドAPI変更 | api-spec.md, data-model.md |
| 在庫・引当 | specs/inventory.md |
| 注文フロー | specs/order.md |
| 認証・認可 | specs/authentication.md |
| 商品・価格 | specs/product.md |
| BFF/プロキシ | specs/bff-architecture.md |
| DBスキーマ変更 | data-model.md |
| UIスタイリング | design-system.md |

---

## インフラ構成詳細

### サービス一覧

| サービス | 技術 | ポート | 備考 |
|---|---|---|---|
| Frontend (Customer) | React 19 + Vite | 5173 | |
| Frontend (Admin) | React 19 + Vite | 5174 | |
| Customer BFF | NestJS | 3001 | |
| BackOffice BFF | NestJS | 3002 | |
| Core API | Spring Boot 3.4.2 | 8080 | 内部ネットワークのみ |
| PostgreSQL | 16 | 5432 | 永続データ |
| Redis | 7.2 | 6379 | キャッシュ/セッション/レート制限（揮発可能） |
| OTel Collector | otel/opentelemetry-collector-contrib | 4317(gRPC) / 4318(HTTP) | |
| Jaeger | jaegertracing/all-in-one | 16686 | トレース UI |
| Prometheus | prom/prometheus | 9090 | メトリクス収集 |
| Grafana | grafana/grafana | 3000 | ダッシュボード |

### データ配置原則

- **PostgreSQL**: 真実のソース（全永続データ）
- **Redis**: Read-Through Cache + セッション + レート制限カウンター（揮発可能）
  - キャッシュTTL: 商品一覧3分・商品詳細10分・在庫1分・認証トークン1分・セッション30分アイドル
  - Redis障害時: Core API から直接取得（性能劣化のみ）

---

## バックエンドアーキテクチャ

### モジュラーモノリス構造

- パッケージ: `modules/{module}/domain|adapter|application`
- 主要モジュール: product / inventory / purchase（cart+order）/ customer / backoffice / shared
- **制約**:
  - 他モジュールの `domain.*` 直接参照禁止。`application.port.*` 経由のみ
  - UseCase 実装クラスはパッケージプライベート（`class`、`public` 不可）
  - クロスモジュール JPA 関連禁止。参照は ID のみ
- ArchUnit による境界制約テスト（10ルール）
- 監査ログはイベント駆動（`OperationPerformedEvent` + REQUIRES_NEW）

---

## 観測性（Observability）

- **トレース**: W3C `traceparent` でフロント〜BFF〜CoreAPI〜DBまで一気通貫。Jaeger UI (localhost:16686) で確認
  - Browser → OTel Collector (OTLP/HTTP:4318)
  - NestJS BFF / Spring Boot → OTel Collector (OTLP/gRPC:4317)
- **メトリクス**: Prometheus (localhost:9090) + Grafana (localhost:3000)
  - NestJS BFF: OTLP → Collector → Prometheus pull (:8889)
  - Spring Boot: `/actuator/prometheus` を Prometheus が直接スクレイプ
- **ビジネスメトリクス**: 注文成功/失敗 Counter、在庫引当失敗 Counter、認証失敗 Counter、注文処理時間 Timer（`OrderUseCase` / `AuthService`）
- **ログ相関**: traceId/spanId をログに付与（NestJS: OTel API、Spring: Micrometer Tracing → MDC）
