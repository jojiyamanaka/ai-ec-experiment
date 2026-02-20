# アーカイブ（完了済み変更案件）

このディレクトリには、実装が完了し主要ドキュメントに反映された変更案件（CHG-XXX）のドキュメントが保管されています。

## 目的

- **主要ドキュメントを Single Source of Truth として維持**: 仕様はSPEC.md、requirements.md、data-model.mdなどに集約
- **詳細な履歴・背景を保持**: CHG案件の詳細な要件定義、設計、実装タスクは参照可能
- **ドキュメントディレクトリの整理**: 現在進行中の案件と完了済み案件を分離

## ディレクトリ構成

```
archive/
├── 01_requirements/     # 要件定義ドキュメント
├── 02_designs/          # 技術設計ドキュメント
├── 03_tasks/            # 実装タスクドキュメント
└── 04_impl-notes/       # 実装判断メモ
```

## アーカイブ済み案件

### CHG-002: 非公開商品カート整合性
- **実装完了日**: 2026-02-11
- **内容**: 商品が非公開になった際にカートから自動除外する機能
- **主要ドキュメント反映先**:
  - data-model.md: CartItem に非公開商品除外ルール追加
  - requirements.md: カート機能のビジネスルール追加

### CHG-003: カート数量上限
- **実装完了日**: 2026-02-11
- **内容**: 1商品あたりの購入数量を1〜9個に制限
- **主要ドキュメント反映先**:
  - data-model.md: CartItem の数量制約更新
  - requirements.md: カート機能のビジネスルール追加
  - ui/customer-ui.md: カート画面の数量制限仕様更新

### CHG-004: エラーメッセージ改善
- **実装完了日**: 2026-02-11
- **内容**: ユーザーフレンドリーなエラーメッセージへの改善
- **主要ドキュメント反映先**: 実装のみの改善のためドキュメント影響なし

### CHG-005: エディトリアルデザイン
- **実装完了日**: 2026-02-12
- **内容**: UI全体をエディトリアルデザインに刷新（モノトーン、セリフ体、広い余白）
- **主要ドキュメント反映先**:
  - ui/customer-ui.md: デザインシステムセクション全面刷新
  - design-system.md: 新規作成（デザインシステム仕様書）

### CHG-006: 会員機能追加
- **実装完了日**: 2026-02-12
- **内容**: トークンベース認証、ロールベースアクセス制御（RBAC）、操作履歴
- **主要ドキュメント反映先**:
  - data-model.md: User、AuthToken、OperationHistory エンティティ追加
  - api-spec.md: 認証API（5エンドポイント）追加
  - requirements.md: セッション管理を認証セクションに拡張
  - SPEC.md: 認証・認可セクション追加、Phase制約更新
  - specs/authentication.md: 新規作成（認証詳細仕様書）
  - specs/order.md: 会員/ゲスト区別、注文履歴取得追加
  - ui/admin-ui.md: 認証・認可セクション追加

### CHG-007: 管理画面拡張（注文・在庫・会員）
- **実装完了日**: 2026-02-13
- **内容**: 管理画面に注文管理、在庫管理、会員管理の3機能を追加
- **主要ドキュメント反映先**:
  - ui/admin-ui.md: 注文管理、在庫管理、会員管理の画面仕様追加
  - api-spec.md: 管理API（注文、在庫、会員）追加

### CHG-008: ドメイン分離とBoUser管理
- **実装完了日**: 2026-02-13
- **内容**: 顧客（Customer）と管理者（BoUser）のドメイン完全分離、BoUser専用認証基盤の構築
- **主要ドキュメント反映先**:
  - SPEC.md: ドメイン分離、BoUser認証、権限レベル、セキュリティ強化を追加
  - data-model.md: BoUser、BoAuthToken エンティティ追加（予定）
  - api-spec.md: BoAuth API、管理API（/api/bo/**）の仕様追加（予定）

### CHG-011: 商品一覧ページネーション
- **実装完了日**: 2026-02-18
- **内容**: 商品一覧画面にページネーションUI追加（1ページ12件、URL連動）
- **主要ドキュメント反映先**:
  - ui/customer-ui.md: 商品一覧画面にページネーション仕様追記
  - spec-implementation-gaps.md: L-1を実装完了に更新

### CHG-013: モジュラーモノリス設計
- **実装完了日**: 2026-02-18
- **内容**: パッケージ構造をモジュール別に再編、Port/UseCase パターン導入、ArchUnit 境界テスト追加、監査ログのイベント駆動化
- **主要ドキュメント反映先**:
  - SPEC.md: アーキテクチャ変更履歴にモジュール構造・制約を追加

### CHG-012: API契約管理
- **実装完了日**: 2026-02-18
- **内容**: springdoc-openapi導入、全コントローラ/DTOにOpenAPIアノテーション付与、Swagger UI公開
- **主要ドキュメント反映先**: ツーリング追加のみのためドキュメント影響なし

### CHG-015: OpenTelemetry 導入
- **実装完了日**: 2026-02-19
- **内容**: OTel Collector / Jaeger / Prometheus / Grafana 導入、全レイヤの分散トレーシング・ビジネスメトリクス計測基盤を構築
- **主要ドキュメント反映先**:
  - SPEC.md: アーキテクチャ変更履歴にOTel観測スタックと既存変更を追加
  - specs/bff-architecture.md: 共通機能セクションにOTel SDK統合・インターセプタ変更を反映

### CHG-014: Redis導入とBFF機能拡張
- **実装完了日**: 2026-02-18
- **内容**: Redis 7.2導入、BFFにキャッシュ層・セッション管理・レート制限・レスポンス集約エンドポイントを実装
- **主要ドキュメント反映先**:
  - SPEC.md: アーキテクチャ変更履歴にRedis導入・データ配置原則を追加
  - specs/bff-architecture.md: 設計原則・エンドポイント・モジュール構成・レート制限を更新
  - ui/api-spec.md: BFF集約エンドポイントセクション追加
  - specs/authentication.md: BFF認証トークンキャッシュ仕様を追加

### CHG-016: フロントエンドをFSD（Feature-Sliced Design）構成へ移行
- **実装完了日**: 2026-02-19
- **内容**: フロントエンドを FSD レイヤ（app/pages/widgets/features/entities/shared）に再編、依存ルール（上位→下位）を ESLint で強制
- **主要ドキュメント反映先**: フロントエンド内部構成のみのためドキュメント影響なし

### CHG-017: 非同期処理（監査ログ・メール送信）
- **実装完了日**: 2026-02-19
- **内容**: Transactional Outbox パターンで非同期処理基盤を構築。Mailpit でメール送信、OutboxEvent テーブルで失敗時の再試行・DLQ管理を実現
- **主要ドキュメント反映先**:
  - SPEC.md: インフラ構成詳細（Mailpit追加）、バックエンドアーキテクチャ（Outbox パターン記載）
  - data-model.md: OutboxEvent エンティティ・スキーマ追加
  - backend/AGENTS.md: shared/outbox サブパッケージ構成追加

### CHG-018: 業務ジョブ基盤（JobRunr）導入
- **実装完了日**: 2026-02-19
- **内容**: JobRunr を導入し、cron→統一ジョブ基盤へ移行。仮引当解除・出荷指示連携（直列3段階）を実装。Shipment ドメイン新規追加
- **主要ドキュメント反映先**:
  - SPEC.md: バックエンドアーキテクチャにJobRunrジョブ管理を記載
  - data-model.md: Shipment、ShipmentItem、JobRunHistory エンティティ・スキーマ追加
  - specs/order.md: PREPARING_SHIPMENT ステータス追加、ステータス遷移図更新
  - ui/api-spec.md: `/api/order/:id/ship` 削除、`/api/order/:id/mark-shipped` 追加
  - backend/AGENTS.md: shared/job と各モジュール job パッケージ構成追加

### CHG-019: BFF OpenAPI 導入（@nestjs/swagger）
- **実装完了日**: 2026-02-19
- **内容**: Customer BFF・BackOffice BFF に `@nestjs/swagger` を導入。全エンドポイント・DTO の OpenAPI spec 自動生成、Swagger UI（`/api-docs`）を実装。ローカル開発・CI で spec を自動更新
- **主要ドキュメント反映先**:
  - ui/api-spec.md: BFF OpenAPI spec が SSOT である点を冒頭に明示
  - specs/bff-architecture.md: OpenAPI spec 参照セクション追加、開発フロー記載

### CHG-020: User データモデル項目追加と画面反映
- **実装完了日**: 2026-02-20
- **内容**: `users` テーブルに会員拡張項目を追加し、新設 `user_addresses` で複数住所を管理。顧客マイページ新設、BO会員FULL更新/新規登録、許可外フィールド拒否の契約を実装
- **主要ドキュメント反映先**:
  - data-model.md: users 拡張カラム・user_addresses エンティティ追加
  - requirements.md: 会員情報管理・住所管理のビジネスルール追加
  - ui/customer-ui.md: マイページ（会員情報更新・住所管理）仕様追加
  - ui/admin-ui.md: 会員FULL更新・新規登録UI仕様追加
  - specs/bff-architecture.md: Customer BFF 住所CRUD・BO 会員作成/FULL更新エンドポイント追加
  - docs/api/*.json: 各 OpenAPI spec を CHG-020 契約に更新

### CHG-021: Product データモデル項目追加と画面反映
- **実装完了日**: 2026-02-20
- **内容**: 商品マスタに品番・カテゴリ参照・公開/販売期間を追加し、`product_categories` を新設。管理画面の商品新規登録/カテゴリ管理、顧客向け公開判定（商品公開×カテゴリ公開×期間）を実装
- **主要ドキュメント反映先**:
  - data-model.md: `products` 拡張カラム、`product_categories` 追加
  - requirements.md: 公開表示/購入可否の判定式、期間制約ルール追加
  - specs/product.md: 商品・カテゴリ・公開/販売期間仕様を更新
  - ui/customer-ui.md: 公開表示条件とカート除外条件を更新
  - ui/admin-ui.md: 商品登録/カテゴリ管理/公開・販売日時運用を更新
  - docs/api/*.json: Core/BFF OpenAPI 契約を CHG-021 に更新

### CHG-022: BO管理画面 認証復元と商品初期表示不具合修正
- **実装完了日**: 2026-02-20
- **内容**: BackOffice BFF に `GET /api/bo-auth/me` を追加し、管理画面の認証復元フローを安定化。未認証時の商品先行フェッチ停止と商品初期表示の再取得制御を実装
- **主要ドキュメント反映先**:
  - specs/bff-architecture.md: `/api/bo-auth/me` 契約と認証復元フローを追記
  - specs/authentication.md: BoUser 認証復元時の 401 契約（`BFF_UNAUTHORIZED`/`BFF_INVALID_TOKEN`）を追記
  - ui/admin-ui.md: 管理画面リロード時の認証復元挙動を追記

### CHG-023: 引当区分導入と在庫モデル再編
- **実装完了日**: 2026-02-20
- **内容**: 商品ごとに `allocationType`（`REAL`/`FRAME`）を導入し、在庫源泉を `location_stocks` / `sales_limits` へ分離。枠在庫商品の非同期本引当、注文進捗（`allocatedQuantity / orderedQuantity`）可視化、商品詳細3タブ化と `/bo/inventory` 導線廃止を実装
- **主要ドキュメント反映先**:
  - data-model.md: `allocation_type`、`location_stocks`、`sales_limits`、`order_items.allocated_qty` を反映
  - requirements.md: `effectiveStock` 基準、出荷統制（全量引当済み）を反映
  - specs/product.md: `allocationType` / `effectiveStock` 契約反映
  - specs/inventory.md: 在庫源泉分離と非同期本引当フローを反映
  - specs/order.md: 進捗項目と引当再試行 API を反映
  - specs/bff-architecture.md: 管理向け在庫タブ API・本引当再試行 API を反映
  - ui/admin-ui.md: 商品詳細3タブ構成、`/bo/inventory` 導線廃止を反映
  - docs/api/*.json: Core/BFF OpenAPI 契約を CHG-023 に更新

## 参照方法

アーカイブされた案件を参照する場合:

```bash
# 特定案件の要件定義を確認
cat docs/archive/01_requirements/CHG-006_会員機能追加.md

# 特定案件の技術設計を確認
cat docs/archive/02_designs/CHG-006_Task1_会員登録とログイン基盤.md

# 特定案件のタスク一覧を確認
ls docs/archive/03_tasks/CHG-006*.md
```

## 注意事項

- **アーカイブされたドキュメントは編集しない**: 主要ドキュメントが正式な仕様
- **履歴参照目的**: なぜその仕様になったのか、どのように実装したのかを振り返る目的で参照
- **新規機能開発**: 主要ドキュメント（SPEC.md、requirements.md など）を基に進める

## 関連ドキュメント

- [CLAUDE.md](../../CLAUDE.md) - プロジェクトガイド、機能開発プロセス、アーカイブルール
- [SPEC.md](../SPEC.md) - 技術仕様書（Single Source of Truth）
- [requirements.md](../requirements.md) - 業務要件・ビジネスルール
- [data-model.md](../data-model.md) - データモデル・エンティティ定義
