# CHG-013: モジュラーモノリス設計 Phase 4 - 実装タスク

要件: `docs/01_requirements/CHG-013_モジュラーモノリス設計`
設計: `docs/02_designs/CHG-013_モジュラーモノリス設計.md`
作成日: 2026-02-18

検証コマンド:
- バックエンド: `docker compose exec backend ./mvnw compile`
- 全テスト: `docker compose exec backend ./mvnw test`
- コンテナ未起動の場合: `docker compose up -d` を先に実行

---

## Phase 4 概要

Phase 4では、監査ログ記録を Spring Application Events を使った非同期処理に移行します。

**対象**: 監査ログ記録（OperationHistory）

**移行方針**:
1. ドメインイベント定義（DomainEvent, OperationPerformedEvent）
2. イベントハンドラ作成（REQUIRES_NEW トランザクション）
3. 各モジュールでのイベント発行（ApplicationEventPublisher）
4. 既存の同期ログ記録を削除
5. テスト実行

**重要**: 監査ログはメイン処理が失敗してもログを残すため、別トランザクション（REQUIRES_NEW）で実行します。

**前提条件**: Phase 3（ArchUnit導入）が完了していること

---

## タスク一覧

### イベント定義

- [ ] **T-1**: イベント用ディレクトリの作成

  ```bash
  mkdir -p backend/src/main/java/com/example/aiec/modules/shared/event
  ```

---

- [ ] **T-2**: DomainEvent インターフェース作成

  パス: `backend/src/main/java/com/example/aiec/modules/shared/event/DomainEvent.java`

  新規ファイル作成:

  ```java
  package com.example.aiec.modules.shared.event;

  import java.time.Instant;

  /**
   * ドメインイベントの基底インターフェース
   */
  public interface DomainEvent {

      /**
       * イベント発生時刻
       */
      Instant occurredAt();

  }
  ```

---

- [ ] **T-3**: OperationPerformedEvent レコード作成

  パス: `backend/src/main/java/com/example/aiec/modules/shared/event/OperationPerformedEvent.java`

  新規ファイル作成:

  ```java
  package com.example.aiec.modules.shared.event;

  import java.time.Instant;

  /**
   * 操作実行イベント（監査ログ記録用）
   *
   * 本処理が失敗してもログを残すため、イベントハンドラは別トランザクション（REQUIRES_NEW）で実行される
   */
  public record OperationPerformedEvent(
      String operationType,
      String performedBy,
      String requestPath,
      String details,
      Instant occurredAt
  ) implements DomainEvent {

      /**
       * コンストラクタ（occurredAt を自動設定）
       */
      public OperationPerformedEvent(
          String operationType,
          String performedBy,
          String requestPath,
          String details
      ) {
          this(operationType, performedBy, requestPath, details, Instant.now());
      }

  }
  ```

---

### イベントハンドラ作成

- [ ] **T-4**: OperationHistoryEventHandler 作成

  パス: `backend/src/main/java/com/example/aiec/modules/backoffice/application/usecase/OperationHistoryEventHandler.java`

  新規ファイル作成:

  ```java
  package com.example.aiec.modules.backoffice.application.usecase;

  import com.example.aiec.modules.backoffice.domain.entity.OperationHistory;
  import com.example.aiec.modules.backoffice.domain.repository.OperationHistoryRepository;
  import com.example.aiec.modules.shared.event.OperationPerformedEvent;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.context.event.EventListener;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Propagation;
  import org.springframework.transaction.annotation.Transactional;

  /**
   * 監査ログイベントハンドラ
   *
   * REQUIRES_NEW で別トランザクションとして実行されるため、
   * メイン処理が失敗してもログは記録される
   */
  @Service
  @RequiredArgsConstructor
  @Slf4j
  class OperationHistoryEventHandler {

      private final OperationHistoryRepository operationHistoryRepository;

      /**
       * 操作実行イベントを受信して監査ログに記録
       */
      @EventListener
      @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
      public void handleOperationPerformed(OperationPerformedEvent event) {
          try {
              OperationHistory history = new OperationHistory();
              history.setOperationType(event.operationType());
              history.setPerformedBy(event.performedBy());
              history.setRequestPath(event.requestPath());
              history.setDetails(event.details());
              history.setPerformedAt(event.occurredAt());

              operationHistoryRepository.save(history);
              log.debug("監査ログを記録しました: operationType={}, performedBy={}",
                      event.operationType(), event.performedBy());
          } catch (Exception e) {
              log.error("監査ログの記録に失敗しました: event={}", event, e);
              // 監査ログ記録失敗はメイン処理に影響させない（ログ出力のみ）
          }
      }

  }
  ```

  **ポイント**:
  - `@Transactional(propagation = Propagation.REQUIRES_NEW)` により、別トランザクションで実行
  - メイン処理が失敗してもこのトランザクションはコミットされる
  - 監査ログ記録の失敗はメイン処理に影響させない（try-catchでログ出力のみ）

---

### 既存OperationHistoryServiceの削除

- [ ] **T-5**: OperationHistoryService の削除

  ```bash
  cd backend/src/main/java/com/example/aiec/modules

  # 既存のOperationHistoryServiceを削除（イベントハンドラに移行）
  git rm shared/domain/service/OperationHistoryService.java
  ```

  **理由**: イベントハンドラに機能が移行したため、既存Serviceクラスは不要になりました。

---

### イベント発行の実装

- [ ] **T-6**: ProductController にイベント発行を追加

  パス: `backend/src/main/java/com/example/aiec/modules/product/adapter/rest/ProductController.java`

  **import文の追加**:

  クラス先頭のimport文に以下を追加:

  ```java
  import com.example.aiec.modules.shared.event.OperationPerformedEvent;
  import org.springframework.context.ApplicationEventPublisher;
  ```

  **フィールド追加**:

  クラス内のフィールド宣言に以下を追加:

  ```java
  private final ApplicationEventPublisher eventPublisher;
  ```

  **updateProduct メソッドの変更**:

  メソッドの最後（`return` の直前）に以下を追加:

  ```java
  // 監査ログ記録イベント発行
  eventPublisher.publishEvent(new OperationPerformedEvent(
      "PRODUCT_UPDATE",
      "admin",  // TODO: 実際の管理者情報に置き換え
      "/api/admin/products/" + id,
      "productId=" + id
  ));
  ```

---

- [ ] **T-7**: InventoryController にイベント発行を追加（必要に応じて）

  パス: `backend/src/main/java/com/example/aiec/modules/inventory/adapter/rest/BoAdminInventoryController.java`

  **import文の追加**:

  ```java
  import com.example.aiec.modules.shared.event.OperationPerformedEvent;
  import org.springframework.context.ApplicationEventPublisher;
  ```

  **フィールド追加**:

  ```java
  private final ApplicationEventPublisher eventPublisher;
  ```

  **adjustStock メソッドの変更**:

  メソッドの最後（`return` の直前）に以下を追加:

  ```java
  // 監査ログ記録イベント発行
  eventPublisher.publishEvent(new OperationPerformedEvent(
      "INVENTORY_ADJUST",
      admin.getEmail(),
      "/api/backoffice/inventory/adjust",
      "productId=" + productId + ", delta=" + quantityDelta
  ));
  ```

---

- [ ] **T-8**: OrderController にイベント発行を追加

  パス: `backend/src/main/java/com/example/aiec/modules/purchase/adapter/rest/OrderController.java`

  **import文の追加**:

  ```java
  import com.example.aiec.modules.shared.event.OperationPerformedEvent;
  import org.springframework.context.ApplicationEventPublisher;
  ```

  **フィールド追加**:

  ```java
  private final ApplicationEventPublisher eventPublisher;
  ```

  **createOrder メソッドの変更**:

  メソッドの最後（`return` の直前）に以下を追加:

  ```java
  // 監査ログ記録イベント発行
  eventPublisher.publishEvent(new OperationPerformedEvent(
      "ORDER_CREATE",
      userId != null ? userId.toString() : "guest",
      "/api/orders",
      "orderId=" + result.getId()
  ));
  ```

  **cancelOrder メソッドの変更**:

  メソッドの最後（`return` の直前）に以下を追加:

  ```java
  // 監査ログ記録イベント発行
  eventPublisher.publishEvent(new OperationPerformedEvent(
      "ORDER_CANCEL",
      userId != null ? userId.toString() : "guest",
      "/api/orders/" + id + "/cancel",
      "orderId=" + id
  ));
  ```

  **confirmOrder メソッドの変更**:

  メソッドの最後（`return` の直前）に以下を追加:

  ```java
  // 監査ログ記録イベント発行
  eventPublisher.publishEvent(new OperationPerformedEvent(
      "ORDER_CONFIRM",
      "admin",  // TODO: 実際の管理者情報に置き換え
      "/api/backoffice/orders/" + id + "/confirm",
      "orderId=" + id
  ));
  ```

  **shipOrder メソッドの変更**:

  メソッドの最後（`return` の直前）に以下を追加:

  ```java
  // 監査ログ記録イベント発行
  eventPublisher.publishEvent(new OperationPerformedEvent(
      "ORDER_SHIP",
      "admin",  // TODO: 実際の管理者情報に置き換え
      "/api/backoffice/orders/" + id + "/ship",
      "orderId=" + id
  ));
  ```

  **deliverOrder メソッドの変更**:

  メソッドの最後（`return` の直前）に以下を追加:

  ```java
  // 監査ログ記録イベント発行
  eventPublisher.publishEvent(new OperationPerformedEvent(
      "ORDER_DELIVER",
      "admin",  // TODO: 実際の管理者情報に置き換え
      "/api/backoffice/orders/" + id + "/deliver",
      "orderId=" + id
  ));
  ```

---

- [ ] **T-9**: AuthController にイベント発行を追加

  パス: `backend/src/main/java/com/example/aiec/modules/customer/adapter/rest/AuthController.java`

  **import文の追加**:

  ```java
  import com.example.aiec.modules.shared.event.OperationPerformedEvent;
  import org.springframework.context.ApplicationEventPublisher;
  ```

  **フィールド追加**:

  ```java
  private final ApplicationEventPublisher eventPublisher;
  ```

  **register メソッドの変更**:

  メソッドの最後（`return` の直前）に以下を追加:

  ```java
  // 監査ログ記録イベント発行
  eventPublisher.publishEvent(new OperationPerformedEvent(
      "USER_REGISTER",
      response.getEmail(),
      "/api/auth/register",
      "userId=" + response.getId()
  ));
  ```

  **login メソッドの変更**:

  メソッドの最後（`return` の直前）に以下を追加:

  ```java
  // 監査ログ記録イベント発行
  eventPublisher.publishEvent(new OperationPerformedEvent(
      "USER_LOGIN",
      response.getEmail(),
      "/api/auth/login",
      "userId=" + response.getId()
  ));
  ```

---

- [ ] **T-10**: BoAuthController にイベント発行を追加

  パス: `backend/src/main/java/com/example/aiec/modules/backoffice/adapter/rest/BoAuthController.java`

  **import文の追加**:

  ```java
  import com.example.aiec.modules.shared.event.OperationPerformedEvent;
  import org.springframework.context.ApplicationEventPublisher;
  ```

  **フィールド追加**:

  ```java
  private final ApplicationEventPublisher eventPublisher;
  ```

  **login メソッドの変更**:

  メソッドの最後（`return` の直前）に以下を追加:

  ```java
  // 監査ログ記録イベント発行
  eventPublisher.publishEvent(new OperationPerformedEvent(
      "BO_USER_LOGIN",
      response.getEmail(),
      "/api/backoffice/auth/login",
      "userId=" + response.getId()
  ));
  ```

---

- [ ] **T-11**: BoAdminBoUsersController にイベント発行を追加

  パス: `backend/src/main/java/com/example/aiec/modules/backoffice/adapter/rest/BoAdminBoUsersController.java`

  **import文の追加**:

  ```java
  import com.example.aiec.modules.shared.event.OperationPerformedEvent;
  import org.springframework.context.ApplicationEventPublisher;
  ```

  **フィールド追加**:

  ```java
  private final ApplicationEventPublisher eventPublisher;
  ```

  **各メソッドの最後（`return` の直前）にイベント発行を追加**:

  - createBoUser: `"BO_USER_CREATE"`
  - updatePermission: `"BO_USER_UPDATE_PERMISSION"`
  - deleteBoUser: `"BO_USER_DELETE"`

---

### 統合作業

- [ ] **T-12**: コンパイル確認

  ```bash
  docker compose exec backend ./mvnw compile
  ```

  コンパイルエラーが出た場合は import 文を確認。

---

- [ ] **T-13**: 全テスト実行確認

  ```bash
  docker compose exec backend ./mvnw test
  ```

  全テストがパスすることを確認。

---

- [ ] **T-14**: 手動テスト: イベント発行確認

  1. Docker コンテナを起動:
     ```bash
     docker compose up -d
     ```

  2. ログを確認:
     ```bash
     docker compose logs -f backend
     ```

  3. 以下のいずれかの操作を実行:
     - 商品更新（管理画面）
     - 注文作成（顧客画面）
     - ログイン（顧客/管理）

  4. ログに以下のメッセージが出力されることを確認:
     ```
     監査ログを記録しました: operationType=ORDER_CREATE, performedBy=...
     ```

  5. データベースの `operation_histories` テーブルに記録が追加されていることを確認:
     ```bash
     docker compose exec postgres psql -U postgres -d ai_ec -c "SELECT * FROM operation_histories ORDER BY performed_at DESC LIMIT 5;"
     ```

---

## 実装順序

```
T-1（イベントディレクトリ作成）
  → T-2, T-3（イベント定義）
    → T-4（イベントハンドラ作成）
      → T-5（既存OperationHistoryService削除）
        → T-6〜T-11（各Controllerにイベント発行追加）（並行可能）
          → T-12（コンパイル確認）
            → T-13（全テスト実行確認）
              → T-14（手動テスト）
```

---

## テスト手順

Phase 4完了後に以下を確認:

1. `docker compose exec backend ./mvnw compile` → コンパイル成功
2. `docker compose exec backend ./mvnw test` → 既存テスト全パス
3. 各Controllerで主要操作時にイベントが発行されている
4. `operation_histories` テーブルにログが記録されている
5. メイン処理が失敗してもログが記録されている（REQUIRES_NEW の効果確認）

---

## トランザクション境界の確認

REQUIRES_NEW が正しく動作しているか確認するテスト手順:

1. 注文作成のメイン処理を意図的に失敗させる（例: 存在しない商品IDを指定）
2. メイン処理は失敗するが、監査ログは記録されることを確認
3. `operation_histories` テーブルに `ORDER_CREATE` のログが残っていることを確認

**期待結果**: メイン処理が失敗しても、監査ログは別トランザクションで記録されているため、ログは残る。

---

## 完了条件

Phase 4完了により、CHG-013（モジュラーモノリス設計）の全Phaseが完了します:

- ✅ Phase 1: パッケージ移動
- ✅ Phase 2: Port抽出
- ✅ Phase 3: ArchUnit導入
- ✅ Phase 4: 監査ログイベント実装

次のステップ:
- Phase 5以降（通知系・集計系イベント）は別途CHG案件として計画予定
