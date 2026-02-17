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
- **[API仕様](./ui/api-spec.md)** - APIエンドポイント、リクエスト/レスポンス仕様

### ドメイン層
- **[商品ドメイン](./specs/product.md)** - 商品マスタ、価格管理、公開制御
- **[在庫ドメイン](./specs/inventory.md)** - 在庫引当（仮引当・本引当）の詳細仕様
- **[注文ドメイン](./specs/order.md)** - 注文フロー、状態遷移の詳細仕様
- **[認証](./specs/authentication.md)** - 顧客/管理者の認証・認可
- **[BFFアーキテクチャ](./specs/bff-architecture.md)** - Customer BFF / BackOffice BFF の構成

### その他
- **[仕様と実装のギャップ一覧](./spec-implementation-gaps.md)** - 未実装機能、制約事項

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

## アーキテクチャ変更履歴

### CHG-010: BFF構成への移行（Phase 3完了）

**変更内容**:
- Customer BFF導入（顧客向けAPI）
- BackOffice BFF導入（管理向けAPI）
- Core APIの内部ネットワーク化

**影響**:
- Core APIへの直接アクセスは不可
- すべてのAPI呼び出しはBFF経由
- 顧客トークンと管理トークンの境界を明確化

**ロールバック手順**: `docs/operations/rollback-procedure.md` 参照
