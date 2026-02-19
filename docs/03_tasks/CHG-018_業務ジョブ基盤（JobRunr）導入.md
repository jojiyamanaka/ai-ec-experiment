# CHG-018: 業務ジョブ基盤（JobRunr）導入 - 実装タスク

要件: `docs/01_requirements/CHG-018_業務ジョブ基盤（JobRunr）導入.md`
設計: `docs/02_designs/CHG-018_業務ジョブ基盤（JobRunr）導入_new-format.md`
作成日: 2026-02-19

---

## タスク一覧

### バックエンド基盤

- [ ] **T-1** `[CONTRACT]`: JobRunr 依存・設定・コンテナ設定を追加

  触る範囲: `backend/pom.xml` / `backend/src/main/resources/application.yml` / `docker-compose.yml`

  Done: `cd backend && ./mvnw compile` が通ること

  > 📝 ゲート高。Codex は impl-notes（変更の概要・判断の根拠）を `docs/impl-notes/CHG-018.md` の `## T-1` セクションに追記し、追加検証コマンドを実行すること。

---

- [ ] **T-2** `[CONTRACT]`: Flyway V8 マイグレーション（新規3テーブル）を作成

  触る範囲: `backend/src/main/resources/db/flyway/V8__add_jobrunr_infrastructure.sql`

  Done: `docker compose up -d` 後に Flyway V8 が正常適用されること（ログに `Successfully applied 1 migration` が出ること）

  > 📝 ゲート高。Codex は impl-notes（変更の概要・判断の根拠）を `docs/impl-notes/CHG-018.md` の `## T-2` セクションに追記し、追加検証コマンドを実行すること。

---

- [ ] **T-3** `[ARCH]`: shared/job 基盤クラス一式を新規作成

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

  > 📝 ゲート高。Codex は impl-notes（変更の概要・判断の根拠）を `docs/impl-notes/CHG-018.md` の `## T-3` セクションに追記し、追加検証コマンドを実行すること。

---

### ドメイン・リポジトリ

- [ ] **T-4** `[CONTRACT]`: purchase/shipment エンティティ・リポジトリを新規作成

  触る範囲:
  - `purchase/shipment/entity/Shipment`
  - `purchase/shipment/entity/ShipmentItem`
  - `purchase/shipment/repository/ShipmentRepository`
  - `purchase/shipment/repository/ShipmentItemRepository`

  Done: `cd backend && ./mvnw compile` が通ること

  > 📝 ゲート高。Codex は impl-notes（変更の概要・判断の根拠）を `docs/impl-notes/CHG-018.md` の `## T-4` セクションに追記し、追加検証コマンドを実行すること。

---

- [ ] **T-5** `[CONTRACT]`: Order に PREPARING_SHIPMENT 追加・OrderRepository クエリ追加・ポート/ユースケース変更

  触る範囲:
  - `purchase/order/entity/Order`（ステータス enum 変更）
  - `purchase/order/repository/OrderRepository`（クエリ追加）
  - `purchase/application/port/OrderCommandPort`（`shipOrder` 削除 / `markShipped` 追加）
  - `purchase/application/usecase/OrderUseCase`（同上）

  Done: `cd backend && ./mvnw compile` が通ること

  > ⚠️ `shipOrder` の削除は破壊的変更。OrderController との整合を T-6 で確認すること。

  > 📝 ゲート高。Codex は impl-notes（変更の概要・判断の根拠）を `docs/impl-notes/CHG-018.md` の `## T-5` セクションに追記し、追加検証コマンドを実行すること。

---

### コントローラ

- [ ] **T-6** `[CONTRACT]`: OrderController の `/ship` 削除・`/mark-shipped` 追加

  触る範囲: `purchase/adapter/rest/OrderController`

  Done: `cd backend && ./mvnw compile` が通ること

  > 📝 ゲート高。Codex は impl-notes（変更の概要・判断の根拠）を `docs/impl-notes/CHG-018.md` の `## T-6` セクションに追記し、追加検証コマンドを実行すること。

---

### ジョブ実装

- [ ] **T-7** `[SAFE]`: InventoryUseCase の @Scheduled 削除・StockReservationRepository クエリ追加

  触る範囲:
  - `inventory/application/usecase/InventoryUseCase`（`@Scheduled cleanupExpiredReservations` メソッド削除）
  - `inventory/domain/repository/StockReservationRepository`（期限切れ仮引当取得・softDelete クエリ追加）

  Done: `cd backend && ./mvnw compile` が通ること

---

- [ ] **T-8** `[SAFE]`: ReleaseReservationsJob を新規作成

  触る範囲: `inventory/application/job/ReleaseReservationsJob`

  Done: `cd backend && ./mvnw test -Dtest=ReleaseReservationsJobTest` が通ること（SKIPPED 記録・processedCount のケースを含むこと）

---

- [ ] **T-9** `[SAFE]`: CreateShipmentJob を新規作成

  触る範囲: `purchase/application/job/CreateShipmentJob`

  Done: `cd backend && ./mvnw test -Dtest=CreateShipmentJobTest` が通ること（冪等性ケース（2回実行で shipment 1件）を含むこと）

---

- [ ] **T-10** `[SAFE]`: ExportShipmentFileJob を新規作成

  触る範囲: `purchase/application/job/ExportShipmentFileJob`

  Done: `cd backend && ./mvnw compile` が通ること（`order.status` を変更しないこと）

---

- [ ] **T-11** `[SAFE]`: SftpPutJob を新規作成

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

**Final Gate 結果:** （例: 全テスト成功・コンパイル通過）

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
