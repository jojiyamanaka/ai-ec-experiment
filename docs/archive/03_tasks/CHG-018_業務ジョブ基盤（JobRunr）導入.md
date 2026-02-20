# CHG-018: 業務ジョブ基盤（JobRunr）導入 - 実装タスク

要件: `docs/01_requirements/CHG-018_業務ジョブ基盤（JobRunr）導入.md`
設計: `docs/02_designs/CHG-018_業務ジョブ基盤（JobRunr）導入_new-format.md`
作成日: 2026-02-19

---

## タスク一覧

### バックエンド基盤

- [x] **T-1** `[CONTRACT]`: JobRunr 依存・設定・コンテナ設定を追加

  触る範囲: `backend/pom.xml` / `backend/src/main/resources/application.yml` / `docker-compose.yml`

  Done: `cd backend && ./mvnw compile` が通ること

  > 📝 ゲート高。Codex は review-note（変更の概要・判断の根拠）を `docs/archive/04_review-note/CHG-018.md` の `## T-1` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-2** `[CONTRACT]`: Flyway V8 マイグレーション（新規3テーブル）を作成

  触る範囲: `backend/src/main/resources/db/flyway/V8__add_jobrunr_infrastructure.sql`

  Done: `docker compose up -d` 後に Flyway V8 が正常適用されること（ログに `Successfully applied 1 migration` が出ること）

  > 📝 ゲート高。Codex は review-note（変更の概要・判断の根拠）を `docs/archive/04_review-note/CHG-018.md` の `## T-2` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-3** `[ARCH]`: shared/job 基盤クラス一式を新規作成

  触る範囲:
  - `shared/job/domain/entity/JobRunHistory`
  - `shared/job/domain/repo/JobRunHistoryRepository`
  - `shared/job/JobRunnerBase`
  - `shared/job/JobProperties`
  - `shared/job/JobRunrConfig`
  - `shared/job/transfer/TransferStrategy`（IF）
  - `shared/job/transfer/LocalFileTransferStrategy`
  - `shared/job/transfer/SftpTransferStrategy`（スタブ）
  - `shared/job/transfer/TransferStrategyFactory`

  Done:
  - `cd backend && ./mvnw compile` が通ること
  - `cd backend && ./mvnw test -Dtest=ArchitectureTest` が通ること

  > ⚠️ shared モジュールへの新パッケージ追加。ArchUnit 境界テストが通ることを確認すること。

  > 📝 ゲート高。Codex は review-note（変更の概要・判断の根拠）を `docs/archive/04_review-note/CHG-018.md` の `## T-3` セクションに追記し、追加検証コマンドを実行すること。

---

### ドメイン・リポジトリ

- [x] **T-4** `[CONTRACT]`: purchase/shipment エンティティ・リポジトリを新規作成

  触る範囲:
  - `purchase/shipment/entity/Shipment`
  - `purchase/shipment/entity/ShipmentItem`
  - `purchase/shipment/repository/ShipmentRepository`
  - `purchase/shipment/repository/ShipmentItemRepository`

  Done: `cd backend && ./mvnw compile` が通ること

  > 📝 ゲート高。Codex は review-note（変更の概要・判断の根拠）を `docs/archive/04_review-note/CHG-018.md` の `## T-4` セクションに追記し、追加検証コマンドを実行すること。

---

- [x] **T-5** `[CONTRACT]`: Order に PREPARING_SHIPMENT 追加・OrderRepository クエリ追加・ポート/ユースケース変更

  触る範囲:
  - `purchase/order/entity/Order`（ステータス enum 変更）
  - `purchase/order/repository/OrderRepository`（クエリ追加）
  - `purchase/application/port/OrderCommandPort`（`shipOrder` 削除 / `markShipped` 追加）
  - `purchase/application/usecase/OrderUseCase`（同上）

  Done: `cd backend && ./mvnw compile` が通ること

  > ⚠️ `shipOrder` の削除は破壊的変更。OrderController との整合を T-6 で確認すること。

  > 📝 ゲート高。Codex は review-note（変更の概要・判断の根拠）を `docs/archive/04_review-note/CHG-018.md` の `## T-5` セクションに追記し、追加検証コマンドを実行すること。

---

### コントローラ

- [x] **T-6** `[CONTRACT]`: OrderController の `/ship` 削除・`/mark-shipped` 追加

  触る範囲: `purchase/adapter/rest/OrderController`

  Done: `cd backend && ./mvnw compile` が通ること

  > 📝 ゲート高。Codex は review-note（変更の概要・判断の根拠）を `docs/archive/04_review-note/CHG-018.md` の `## T-6` セクションに追記し、追加検証コマンドを実行すること。

---

### ジョブ実装

- [x] **T-7** `[SAFE]`: InventoryUseCase の @Scheduled 削除・StockReservationRepository クエリ追加

  触る範囲:
  - `inventory/application/usecase/InventoryUseCase`（`@Scheduled cleanupExpiredReservations` メソッド削除）
  - `inventory/domain/repository/StockReservationRepository`（期限切れ仮引当取得・softDelete クエリ追加）

  Done: `cd backend && ./mvnw compile` が通ること

---

- [x] **T-8** `[SAFE]`: ReleaseReservationsJob を新規作成

  触る範囲: `inventory/application/job/ReleaseReservationsJob`

  Done: `cd backend && ./mvnw test -Dtest=ReleaseReservationsJobTest` が通ること（SKIPPED 記録・processedCount のケースを含むこと）

---

- [x] **T-9** `[SAFE]`: CreateShipmentJob を新規作成

  触る範囲: `purchase/application/job/CreateShipmentJob`

  Done: `cd backend && ./mvnw test -Dtest=CreateShipmentJobTest` が通ること（冪等性ケース（2回実行で shipment 1件）を含むこと）

---

- [x] **T-10** `[SAFE]`: ExportShipmentFileJob を新規作成

  触る範囲: `purchase/application/job/ExportShipmentFileJob`

  Done: `cd backend && ./mvnw compile` が通ること（`order.status` を変更しないこと）

---

- [x] **T-11** `[SAFE]`: SftpPutJob を新規作成

  触る範囲: `purchase/application/job/SftpPutJob`

  Done: `cd backend && ./mvnw compile` が通ること（TRANSFERRED 済みファイルへの2重転送を防止すること）

---

## 実装順序

```
T-1 → T-2 → T-3 → T-4 → T-5 → T-6 → T-7 → T-8 → T-9 → T-10 → T-11
```

---

## 検証

### Per-task 検証コマンド

```bash
# コンパイル確認（各タスク完了時）
cd backend && ./mvnw compile

# ArchUnit 境界テスト（T-3完了後）
cd backend && ./mvnw test -Dtest=ArchitectureTest

# 特定ジョブのユニットテスト
cd backend && ./mvnw test -Dtest=ReleaseReservationsJobTest
cd backend && ./mvnw test -Dtest=CreateShipmentJobTest
```

### Final Gate（全タスク完了後に必ず実行し、結果をこのファイルに貼り付けること）

```bash
# バックエンド全体
cd backend && ./mvnw compile
cd backend && ./mvnw test

# コンテナ起動・マイグレーション確認
docker compose up -d
docker compose logs backend | grep -E "(Flyway|JobRunr)"
```

**Final Gate 結果:** `cd backend && ./mvnw compile` / `cd backend && ./mvnw test` は成功。`docker compose up -d` 後の `docker compose logs backend | grep -E "(Flyway|JobRunr)"` で Flyway 起動ログと JobRunr Dashboard/BackgroundJobServer 起動ログを確認。

---

## テスト手順

1. `docker compose up -d` → `localhost:8000` で JobRunr ダッシュボードが表示されること
2. CONFIRMED 注文（本引当済み）が存在する状態で CreateShipmentJob を手動トリガー → `shipments` に OUTBOUND レコードが1件作成され、`orders.status = PREPARING_SHIPMENT` になること
3. ExportShipmentFileJob を手動トリガー → CSV ファイルが生成され `shipments.status = EXPORTED` になること
4. SftpPutJob を手動トリガー → `sent/` ディレクトリにファイルがコピーされ `shipments.status = TRANSFERRED` になること
5. `POST /api/order/{id}/mark-shipped` → `orders.status = SHIPPED` になること
6. ReleaseReservationsJob を手動トリガー → 期限切れ仮引当が softDelete され `job_run_history` に processedCount が記録されること
7. `app.jobs.enabled.create-shipment=false` で起動 → CreateShipmentJob が SKIPPED として `job_run_history` に記録されること
8. CreateShipmentJob を2回実行 → 同一注文の `shipments` が1件のみ（冪等確認）

## Review Packet
### 変更サマリ（10行以内）
- JobRunr 依存追加、`application.yml` に `org.jobrunr.*` と `app.jobs.*` を追加。
- Flyway V8 で `job_run_history` / `shipments` / `shipment_items` を追加し、`orders.status` に `PREPARING_SHIPMENT` を反映。
- `shared/job` 基盤（`JobRunnerBase`、`JobProperties`、`JobRunrConfig`、transfer strategy）を新規作成。
- `Shipment`/`ShipmentItem` とリポジトリを追加し、`CreateShipmentJob`/`ExportShipmentFileJob`/`SftpPutJob`/`ReleaseReservationsJob` を実装。
- `OrderCommandPort`/`OrderUseCase`/`OrderController` を `mark-shipped` 契約へ変更、`/ship` を削除。
- `InventoryUseCase` の `@Scheduled` クリーンアップを削除し、期限切れ仮引当 softDelete クエリを追加。
- `ReleaseReservationsJobTest` / `CreateShipmentJobTest` / `ArchitectureTest` を追加。

### 変更ファイル一覧
- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/db/flyway/V8__add_jobrunr_infrastructure.sql`
- `docker-compose.yml`
- `backend/src/main/java/com/example/aiec/modules/shared/job/*`
- `backend/src/main/java/com/example/aiec/modules/shared/job/domain/entity/JobRunHistory.java`
- `backend/src/main/java/com/example/aiec/modules/shared/job/domain/repo/JobRunHistoryRepository.java`
- `backend/src/main/java/com/example/aiec/modules/purchase/shipment/entity/*`
- `backend/src/main/java/com/example/aiec/modules/purchase/shipment/repository/*`
- `backend/src/main/java/com/example/aiec/modules/inventory/application/job/ReleaseReservationsJob.java`
- `backend/src/main/java/com/example/aiec/modules/purchase/application/job/CreateShipmentJob.java`
- `backend/src/main/java/com/example/aiec/modules/purchase/application/job/ExportShipmentFileJob.java`
- `backend/src/main/java/com/example/aiec/modules/purchase/application/job/SftpPutJob.java`
- `backend/src/main/java/com/example/aiec/modules/purchase/order/entity/Order.java`
- `backend/src/main/java/com/example/aiec/modules/purchase/order/repository/OrderRepository.java`
- `backend/src/main/java/com/example/aiec/modules/purchase/application/port/OrderCommandPort.java`
- `backend/src/main/java/com/example/aiec/modules/purchase/application/usecase/OrderUseCase.java`
- `backend/src/main/java/com/example/aiec/modules/purchase/adapter/rest/OrderController.java`
- `backend/src/main/java/com/example/aiec/modules/inventory/domain/repository/StockReservationRepository.java`
- `backend/src/main/java/com/example/aiec/modules/inventory/application/usecase/InventoryUseCase.java`
- `backend/src/test/java/com/example/aiec/architecture/ModularMonolithArchitectureTest.java`
- `backend/src/test/java/com/example/aiec/architecture/ArchitectureTest.java`
- `backend/src/test/java/com/example/aiec/modules/inventory/application/job/ReleaseReservationsJobTest.java`
- `backend/src/test/java/com/example/aiec/modules/purchase/application/job/CreateShipmentJobTest.java`
- `docs/archive/04_review-note/CHG-018.md`

### リスクと未解決
- 手動テスト実施のため DBデータ（`orders`/`shipments`/`stock_reservations`）を一部更新・追加している。
- `docker-compose.yml` に手動検証用の環境変数受け口（`APP_JOBS_ENABLED_*`）を追加した。
- `docker compose` 実行時に `version` 廃止警告が出る（既存定義由来、未対応）。

### テスト結果（PASS/FAIL、失敗時は30行以内）
- [PASS] `cd backend && ./mvnw compile`
- [PASS] `cd backend && ./mvnw test -Dtest=ArchitectureTest`
- [PASS] `cd backend && ./mvnw test -Dtest=ReleaseReservationsJobTest`
- [PASS] `cd backend && ./mvnw test -Dtest=CreateShipmentJobTest`
- [PASS] `cd backend && ./mvnw test`
- [PASS] `docker compose up -d` / `docker compose logs backend | grep -E '(Flyway|JobRunr)'`
- [PASS] 手順2: CreateShipmentJob 手動トリガーで `shipments` 生成と `orders.status=PREPARING_SHIPMENT` を確認
- [PASS] 手順3: ExportShipmentFileJob 手動トリガーで `shipments.status=EXPORTED` と CSV 生成を確認（`sftp-put` 無効化起動で確認）
- [PASS] 手順4: SftpPutJob 手動トリガーで `shipments.status=TRANSFERRED` と `/tmp/aiec/shipments/sent` 配置を確認
- [PASS] 手順5: `POST /api/order/124/mark-shipped` で `orders.status=SHIPPED` を確認
- [PASS] 手順6: ReleaseReservationsJob 手動トリガーで期限切れ仮引当の softDelete と `job_run_history.processed_count` 記録を確認
- [PASS] 手順7: `APP_JOBS_ENABLED_CREATESHIPMENT=false` 起動で `create-shipment` が `SKIPPED` 記録されることを確認
- [PASS] 手順8: CreateShipmentJob を2回実行しても `order_id=124` の OUTBOUND shipment が1件のままを確認
