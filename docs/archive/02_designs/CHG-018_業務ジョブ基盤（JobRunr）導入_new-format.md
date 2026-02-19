# CHG-018: 業務ジョブ基盤（JobRunr）導入 - 技術設計（新フォーマット版）

要件: `docs/01_requirements/CHG-018_業務ジョブ基盤（JobRunr）導入.md`
作成日: 2026-02-19

---

## 1. 設計方針

### JobRunr を採用する理由

要件の「実行履歴管理・失敗時リトライ・手動再実行・ダッシュボード」は `@Scheduled` では満たせない。

| 要件 | @Scheduled | JobRunr |
|------|-----------|---------|
| 実行履歴管理 | ✗ | ✓（DB永続化済み） |
| 自動リトライ | ✗ | ✓（回数・間隔設定可） |
| 手動再実行 | ✗ | ✓（ダッシュボード/API） |
| 排他制御（多重起動防止） | ✗ | ✓（recurring jobは1インスタンス） |
| 直列ジョブチェーン | ✗ | ✓（`BackgroundJob.enqueue` 連携） |
| SKIPPED 記録 | ✗ | ✓（カスタムテーブルで補完） |

### 基本原則

- **同一バイナリ**: `core-web`（API）と `core-worker`（ジョブ実行）は同一 jar を profiles で分離
- **JobRunr 標準機能を活用**: retry / dashboard は JobRunr に委ねる
- **カスタム `job_run_history` で補完**: processedCount・env・SKIPPED など JobRunr に存在しない業務メトリクスのみ独自テーブルに記録
- **冪等設計**: 各ジョブは再実行しても副作用が重複しない
- **設定駆動**: `app.jobs.enabled.<job-name>` で環境ごとに有効/無効を制御

### 出荷ドメインの概念

注文確定（CONFIRMED）後、在庫本引当済みの注文を対象に出荷作業単位として `Shipment` を生成する。
1注文に対して複数の `Shipment` を持てる 1:N 設計（通常出荷 + 将来の返品回収）。

```
orders (1) ──── (N) shipments
                      │
order_items (1:N) ── (1:N) shipment_items
```

`shipments.shipment_type` で出荷種別を区別:

| shipment_type | 意味 | 今回の実装 |
|--------------|------|----------|
| OUTBOUND | 通常出荷（顧客への配送） | ✓ |
| RETURN | 返品回収 | 将来実装 |

### CHG-017 との関係

CHG-017（Outbox）が先に実装される前提。本 CHG の Flyway は **V8** から開始。

### 設計上の制約

- **JobRunr テーブル**: `skip-create: false` で起動時自動生成。`baseline-on-migrate: true` が設定済みのため Flyway と混在しても問題なし
- **CreateShipmentJob のトランザクション粒度**: 現在は全件1トランザクション。大量件数の場合はチャンク分割（別タスク）
- **ArchUnit 境界**: `purchase/application/job/` は purchase モジュールの application 層。`shared/job/` は shared モジュール内。制約違反なし

---

## 2. API契約

| エンドポイント | 変更内容 | 変更前ステータス遷移 | 変更後ステータス遷移 |
|--------------|---------|------------------|------------------|
| `POST /api/order/{id}/ship` | **削除** | CONFIRMED → SHIPPED | バッチが担うため廃止 |
| `POST /api/order/{id}/mark-shipped` | **新規追加** | - | PREPARING_SHIPMENT → SHIPPED |
| `POST /api/order/{id}/confirm` | 変更なし | PENDING → CONFIRMED | 変更なし |
| `POST /api/order/{id}/deliver` | 変更なし | SHIPPED → DELIVERED | 変更なし |

> **フロントエンド影響**: 管理画面の「発送」ボタン削除・`PREPARING_SHIPMENT` 表示追加・「発送完了」ボタン追加は別 CHG で対応。

---

## 3. モジュール・レイヤ構成

```
shared/job/
  domain/entity/      - JobRunHistory エンティティ
  domain/repo/        - JobRunHistoryRepository
  JobRunnerBase       - ジョブ基底クラス（履歴記録）
  JobProperties       - ConfigurationProperties
  JobRunrConfig       - Recurring ジョブ登録
  transfer/
    TransferStrategy         - SFTP転送戦略 IF
    LocalFileTransferStrategy - dev用ローカルコピー
    SftpTransferStrategy      - prod用スタブ（将来実装）
    TransferStrategyFactory   - 戦略セレクター

inventory/
  domain/repository/  - StockReservationRepository（変更）
  application/job/    - ReleaseReservationsJob（新規）
  application/usecase/ - InventoryUseCase（変更: @Scheduled削除）

purchase/
  order/entity/       - Order（変更: PREPARING_SHIPMENT追加）
  shipment/entity/    - Shipment, ShipmentItem（新規パッケージ）
  shipment/repository/ - ShipmentRepository, ShipmentItemRepository（新規）
  order/repository/   - OrderRepository（変更: クエリ追加）
  application/port/   - OrderCommandPort（変更: shipOrder削除, markShipped追加）
  application/usecase/ - OrderUseCase（変更: shipOrder削除, markShipped追加）
  application/job/    - CreateShipmentJob, ExportShipmentFileJob, SftpPutJob（新規）
  adapter/rest/       - OrderController（変更: /ship削除, /mark-shipped追加）
```

**orders.status の状態遷移:**

```
PENDING → CONFIRMED → PREPARING_SHIPMENT → SHIPPED → DELIVERED
            ↑                ↑                 ↑
         管理者手動        Job（CreateShipment）   管理者手動
         (既存)           Shipment作成後に遷移     (mark-shipped)
```

> `PREPARING_SHIPMENT` への遷移はバッチジョブのみ。出荷進捗は `shipments.status` で追跡し、`orders.status` は変更しない。

**DBスキーマ（新規3テーブル）:**

| テーブル | 用途 |
|---------|------|
| `job_run_history` | 業務メトリクス補完（processedCount・env・SKIPPED） |
| `shipments` | 出荷単位（1注文:N出荷、shipment_type で種別管理） |
| `shipment_items` | 出荷明細（order_items のスナップショット） |

---

## 4. 主要クラス/IFの責務

| クラス/IF | 責務 | レイヤ |
|-----------|------|--------|
| `JobRunnerBase` | ジョブ共通処理（enabled判定・履歴記録・成功/失敗マーク） | shared/job |
| `JobProperties` | `app.jobs.*` の設定バインド（有効化・スケジュール・エクスポート・SFTP設定） | shared/job |
| `JobRunrConfig` | Recurring ジョブの登録（スケジュール設定から cron 式を読む） | shared/job |
| `TransferStrategy` | ファイル転送の戦略 IF（`transfer(Path)`） | shared/job/transfer |
| `TransferStrategyFactory` | `app.jobs.sftp.strategy` の値で実装を選択 | shared/job/transfer |
| `ReleaseReservationsJob` | 仮引当解除（期限切れ TENTATIVE を softDelete） | inventory/application/job |
| `CreateShipmentJob` | CONFIRMED注文から Shipment(OUTBOUND) を生成し PREPARING_SHIPMENT に遷移 | purchase/application/job |
| `ExportShipmentFileJob` | READY状態のShipmentからCSVを生成し EXPORTED に遷移 | purchase/application/job |
| `SftpPutJob` | EXPORTED ファイルを TransferStrategy で転送し TRANSFERRED に遷移 | purchase/application/job |
| `Shipment` | 出荷エンティティ（type: OUTBOUND/RETURN, status: READY/EXPORTED/TRANSFERRED） | purchase/shipment/entity |
| `OrderCommandPort` | `markShipped(Long orderId): OrderDto`（`shipOrder` は削除） | purchase/application/port |

**Job① CreateShipmentJob の実行条件（冪等）:**

- `Order.status = CONFIRMED`
- かつ対象注文に `shipment_type = OUTBOUND` の shipments が未存在
- かつ `stock_reservations` に `type = COMMITTED` のレコードが存在

---

## 5. トランザクション・非同期方針

| 項目 | 方針 |
|------|------|
| ジョブ実行モデル | JobRunr の Recurring Job（cron 式）+ enqueue による連鎖 |
| リトライ | JobRunr 標準（`@Job(retries=3)`）に委ねる。指数バックオフ |
| 冪等性 | `shipments(order_id, shipment_type)` にUNIQUE制約。同一ファイル名 TRANSFERRED チェック |
| トランザクション境界 | 各ジョブメソッド単位（`@Transactional`）。全件1トランザクション（将来チャンク化検討） |
| core-web / core-worker 分離 | `JOBRUNR_WORKER_ENABLED` 環境変数で制御。false=REST APIのみ、true=ジョブ実行ワーカー |
| スケジュール設定 | `app.jobs.schedule.<job-name>` の cron 式から JobRunrConfig が登録 |
| 無効化 | `app.jobs.enabled.<job-name>=false` のとき SKIPPED として `job_run_history` に記録し即return |

---

## 6. 処理フロー

### core-web / core-worker 分離

```
JOBRUNR_WORKER_ENABLED=false (core-web)
  → REST API のみ。ジョブはキューに積める、実行しない

JOBRUNR_WORKER_ENABLED=true (core-worker)
  → ジョブ実行ワーカー + JobRunr ダッシュボード(:8000)

共有: PostgreSQL（jobrunr_jobs テーブル経由でキューを共有）
```

### 出荷指示連携フロー

```
[毎日 01:00 RecurringJob]
        ↓
CreateShipmentJob
  CONFIRMED かつ OUTBOUND未作成の注文を対象
  → Shipment(OUTBOUND) + ShipmentItems 作成（status=READY）
  → order → PREPARING_SHIPMENT
  → enqueue(ExportShipmentFileJob)
        ↓
ExportShipmentFileJob
  READY状態のShipmentからCSV生成（tmp→アトミックリネーム）
  → shipments → EXPORTED（order.statusは変更しない）
  → enqueue(SftpPutJob)
        ↓
SftpPutJob
  EXPORTEDファイルを TransferStrategy で転送
  → shipments → TRANSFERRED
  失敗時: JobRunr が retries=3 でリトライ

[WMS処理完了後: 管理者手動]
        ↓
POST /api/order/{id}/mark-shipped
  order: PREPARING_SHIPMENT → SHIPPED
```

### 仮引当解除フロー

```
[5分ごと RecurringJob]
        ↓
ReleaseReservationsJob
  期限切れ TENTATIVE 仮引当を softDelete（冪等）
  → job_run_history に processedCount 記録
```

---

## 7. 影響範囲

| 区分 | 対象（クラス名） | 変更概要 |
|------|----------------|---------|
| 新規作成 | `shared/job/JobRunnerBase` | ジョブ基底クラス |
| 新規作成 | `shared/job/JobProperties` | 設定バインド |
| 新規作成 | `shared/job/JobRunrConfig` | Recurringジョブ登録 |
| 新規作成 | `shared/job/domain/entity/JobRunHistory` | エンティティ |
| 新規作成 | `shared/job/domain/repo/JobRunHistoryRepository` | リポジトリ |
| 新規作成 | `shared/job/transfer/TransferStrategy` | 転送戦略 IF |
| 新規作成 | `shared/job/transfer/LocalFileTransferStrategy` | ローカルコピー実装 |
| 新規作成 | `shared/job/transfer/SftpTransferStrategy` | SFTPスタブ（将来実装） |
| 新規作成 | `shared/job/transfer/TransferStrategyFactory` | 戦略セレクター |
| 新規作成 | `inventory/application/job/ReleaseReservationsJob` | 仮引当解除ジョブ |
| 新規作成 | `purchase/shipment/entity/Shipment` | 出荷エンティティ |
| 新規作成 | `purchase/shipment/entity/ShipmentItem` | 出荷明細エンティティ |
| 新規作成 | `purchase/shipment/repository/ShipmentRepository` | リポジトリ |
| 新規作成 | `purchase/shipment/repository/ShipmentItemRepository` | リポジトリ |
| 新規作成 | `purchase/application/job/CreateShipmentJob` | 出荷準備ジョブ |
| 新規作成 | `purchase/application/job/ExportShipmentFileJob` | 出荷ファイル作成ジョブ |
| 新規作成 | `purchase/application/job/SftpPutJob` | SFTP配置ジョブ |
| 新規作成 | `db/flyway/V8__add_jobrunr_infrastructure.sql` | job_run_history / shipments / shipment_items |
| 既存変更 | `inventory/domain/repository/StockReservationRepository` | 期限切れ仮引当取得・softDelete クエリ追加 |
| 既存変更 | `inventory/application/usecase/InventoryUseCase` | `@Scheduled cleanupExpiredReservations` 削除 |
| 既存変更 | `purchase/order/entity/Order` | `PREPARING_SHIPMENT` ステータス追加（`READY_FOR_SHIP`/`INSTRUCTED` を統合） |
| 既存変更 | `purchase/order/repository/OrderRepository` | `findConfirmedWithoutOutboundShipment` クエリ追加 |
| 既存変更 | `purchase/application/port/OrderCommandPort` | `shipOrder` 削除 / `markShipped` 追加 |
| 既存変更 | `purchase/application/usecase/OrderUseCase` | `shipOrder` 削除 / `markShipped` 追加 |
| 既存変更 | `purchase/adapter/rest/OrderController` | `/ship` 削除 / `/mark-shipped` 追加 |
| 既存変更 | `docker-compose.yml` | ポート8000追加・`JOBRUNR_WORKER_ENABLED` 環境変数追加 |
| 既存変更 | `backend/pom.xml` | `jobrunr-spring-boot-3-starter` 依存追加 |
| 既存変更 | `backend/src/main/resources/application.yml` | JobRunr設定・`app.jobs.*` 設定追加 |
| 影響なし | フロントエンド（顧客画面） | API仕様変更なし |
| 影響なし | Customer BFF | エンドポイント変更なし |
| 影響なし | BackOffice BFF | `/ship` 廃止は 404 許容で対応 |

---

## 8. テスト観点

- 正常系: JobRunr ダッシュボード（localhost:8000）が起動すること
- 正常系: CONFIRMED注文に対し CreateShipmentJob が shipments(OUTBOUND) を1件作成すること
- 正常系: CreateShipmentJob 後に order が PREPARING_SHIPMENT になること
- 正常系: ExportShipmentFileJob が CSV を生成し shipments が EXPORTED になること
- 正常系: CSV出力後も order.status は PREPARING_SHIPMENT のまま変化しないこと
- 正常系: SftpPutJob が LocalFileTransferStrategy で sent/ へコピーすること
- 正常系: `POST /mark-shipped` で PREPARING_SHIPMENT → SHIPPED に遷移すること
- 正常系: ReleaseReservationsJob が期限切れ仮引当を softDelete すること
- 冪等性: CreateShipmentJob を2回実行しても同一注文の shipments が1件のみ
- 冪等性: SftpPutJob を2回実行しても TRANSFERRED が増えないこと
- 無効化: `app.jobs.enabled.create-shipment=false` で SKIPPED が job_run_history に記録されること
- 本引当なし: 本引当未確定の CONFIRMED 注文がスキップされること
- リトライ: SftpPutJob 失敗時に JobRunr の retry_count が増加すること
- アトミック書き込み: CSV生成途中で停止しても .tmp ファイルが公開されないこと
