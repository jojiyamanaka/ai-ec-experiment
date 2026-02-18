# CHG-013: モジュラーモノリス設計 Phase 2 - 実装タスク

要件: `docs/01_requirements/CHG-013_モジュラーモノリス設計`
設計: `docs/02_designs/CHG-013_モジュラーモノリス設計.md`
作成日: 2026-02-18

検証コマンド:
- バックエンド: `docker compose exec backend ./mvnw compile`
- 全テスト: `docker compose exec backend ./mvnw test`
- コンテナ未起動の場合: `docker compose up -d` を先に実行

---

## Phase 2 概要

Phase 2では、各モジュールのServiceクラスから公開API（Port）を抽出し、モジュール間連携をPort経由にします。

**対象モジュール**: product / inventory / purchase / customer / backoffice

**移行方針**:
1. `application/port/` パッケージに公開インターフェースを作成
2. `application/usecase/` パッケージに実装クラスを作成（既存Serviceクラスをリネーム）
3. 他モジュールからの参照をPort経由に変更
4. Controllerからの参照もPort経由に変更
5. コンパイルエラーがなくなるまで調整
6. 既存テストが全パス

**前提条件**: Phase 1（パッケージ移動）が完了していること

---

## タスク一覧

### 準備

- [ ] **T-0**: Port用ディレクトリ構造の作成

  ```bash
  cd backend/src/main/java/com/example/aiec/modules

  # product モジュール
  mkdir -p product/application/port
  mkdir -p product/application/usecase

  # inventory モジュール
  mkdir -p inventory/application/port
  mkdir -p inventory/application/usecase

  # purchase モジュール
  mkdir -p purchase/application/port
  mkdir -p purchase/application/usecase

  # customer モジュール
  mkdir -p customer/application/port
  mkdir -p customer/application/usecase

  # backoffice モジュール
  mkdir -p backoffice/application/port
  mkdir -p backoffice/application/usecase
  ```

---

### product モジュール

- [ ] **T-1**: ProductQueryPort インターフェース作成

  パス: `backend/src/main/java/com/example/aiec/modules/product/application/port/ProductQueryPort.java`

  新規ファイル作成:

  ```java
  package com.example.aiec.modules.product.application.port;

  import com.example.aiec.modules.product.adapter.dto.ProductDto;
  import com.example.aiec.modules.product.adapter.dto.ProductListResponse;

  /**
   * 商品クエリAPI（公開インターフェース）
   */
  public interface ProductQueryPort {

      /**
       * 商品一覧を取得（公開されている商品のみ）
       */
      ProductListResponse getPublishedProducts(int page, int limit);

      /**
       * 商品詳細を取得（公開されている商品のみ）
       */
      ProductDto getProduct(Long id);

  }
  ```

---

- [ ] **T-2**: ProductCommandPort インターフェース作成

  パス: `backend/src/main/java/com/example/aiec/modules/product/application/port/ProductCommandPort.java`

  新規ファイル作成:

  ```java
  package com.example.aiec.modules.product.application.port;

  import com.example.aiec.modules.product.adapter.dto.ProductDto;
  import com.example.aiec.modules.product.adapter.dto.UpdateProductRequest;

  /**
   * 商品コマンドAPI（公開インターフェース）
   */
  public interface ProductCommandPort {

      /**
       * 商品を更新（管理用）
       */
      ProductDto updateProduct(Long id, UpdateProductRequest request);

  }
  ```

---

- [ ] **T-3**: ProductService → ProductUseCase にリネーム・移動

  ```bash
  cd backend/src/main/java/com/example/aiec/modules/product

  # Service → UseCase にリネーム
  git mv domain/service/ProductService.java application/usecase/ProductUseCase.java
  ```

  **package宣言の更新**:

  `ProductUseCase.java` の先頭を以下に変更:

  ```java
  package com.example.aiec.modules.product.application.usecase;

  import com.example.aiec.modules.product.adapter.dto.ProductDto;
  import com.example.aiec.modules.product.adapter.dto.ProductListResponse;
  import com.example.aiec.modules.product.adapter.dto.UpdateProductRequest;
  import com.example.aiec.modules.product.application.port.ProductCommandPort;
  import com.example.aiec.modules.product.application.port.ProductQueryPort;
  import com.example.aiec.modules.product.domain.entity.Product;
  import com.example.aiec.modules.product.domain.repository.ProductRepository;
  import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
  import lombok.RequiredArgsConstructor;
  import org.springframework.data.domain.Page;
  import org.springframework.data.domain.PageRequest;
  import org.springframework.data.domain.Pageable;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  import java.util.List;
  import java.util.stream.Collectors;

  /**
   * 商品ユースケース（Port実装）
   */
  @Service
  @RequiredArgsConstructor
  class ProductUseCase implements ProductQueryPort, ProductCommandPort {

      private final ProductRepository productRepository;

      @Override
      @Transactional(readOnly = true, rollbackFor = Exception.class)
      public ProductListResponse getPublishedProducts(int page, int limit) {
          // 既存実装のまま
      }

      @Override
      @Transactional(readOnly = true, rollbackFor = Exception.class)
      public ProductDto getProduct(Long id) {
          // メソッド名を getProductById → getProduct に変更
          // 既存実装のまま
      }

      @Override
      @Transactional(rollbackFor = Exception.class)
      public ProductDto updateProduct(Long id, UpdateProductRequest request) {
          // 既存実装のまま
      }

  }
  ```

  **変更点**:
  - クラス名: `ProductService` → `ProductUseCase`
  - `implements ProductQueryPort, ProductCommandPort` を追加
  - `@Override` アノテーションを各メソッドに追加
  - メソッド名: `getProductById` → `getProduct`
  - クラススコープを `public` → `class`（パッケージプライベート）

---

- [ ] **T-4**: ProductController の参照をPort経由に変更

  パス: `backend/src/main/java/com/example/aiec/modules/product/adapter/rest/ProductController.java`

  **import文の変更**:

  変更前:
  ```java
  import com.example.aiec.modules.product.domain.service.ProductService;
  ```

  変更後:
  ```java
  import com.example.aiec.modules.product.application.port.ProductCommandPort;
  import com.example.aiec.modules.product.application.port.ProductQueryPort;
  ```

  **フィールド宣言の変更**:

  変更前:
  ```java
  private final ProductService productService;
  ```

  変更後:
  ```java
  private final ProductQueryPort productQuery;
  private final ProductCommandPort productCommand;
  ```

  **メソッド内の変更**:

  全ての `productService.xxx()` 呼び出しを適切なPort経由に変更:
  - `productService.getPublishedProducts(...)` → `productQuery.getPublishedProducts(...)`
  - `productService.getProductById(...)` → `productQuery.getProduct(...)`
  - `productService.updateProduct(...)` → `productCommand.updateProduct(...)`

---

### inventory モジュール

- [ ] **T-5**: InventoryQueryPort インターフェース作成

  パス: `backend/src/main/java/com/example/aiec/modules/inventory/application/port/InventoryQueryPort.java`

  新規ファイル作成:

  ```java
  package com.example.aiec.modules.inventory.application.port;

  import com.example.aiec.modules.inventory.adapter.dto.AvailabilityDto;
  import com.example.aiec.modules.inventory.adapter.dto.InventoryStatusDto;

  import java.util.List;

  /**
   * 在庫クエリAPI（公開インターフェース）
   */
  public interface InventoryQueryPort {

      /**
       * 有効在庫を取得する
       */
      AvailabilityDto getAvailableStock(Long productId);

      /**
       * 全商品の在庫状況を一括取得
       */
      List<InventoryStatusDto> getAllInventoryStatus();

  }
  ```

---

- [ ] **T-6**: InventoryCommandPort インターフェース作成

  パス: `backend/src/main/java/com/example/aiec/modules/inventory/application/port/InventoryCommandPort.java`

  新規ファイル作成:

  ```java
  package com.example.aiec.modules.inventory.application.port;

  import com.example.aiec.modules.backoffice.domain.entity.BoUser;
  import com.example.aiec.modules.inventory.adapter.dto.ReservationDto;
  import com.example.aiec.modules.inventory.domain.entity.InventoryAdjustment;
  import com.example.aiec.modules.purchase.order.entity.Order;

  /**
   * 在庫コマンドAPI（公開インターフェース）
   */
  public interface InventoryCommandPort {

      /**
       * 仮引当を作成する
       */
      ReservationDto createReservation(String sessionId, Long productId, Integer quantity);

      /**
       * 仮引当を更新する（カート数量変更時）
       */
      ReservationDto updateReservation(String sessionId, Long productId, Integer newQuantity);

      /**
       * 仮引当を解除する（カートから商品削除時）
       */
      void releaseReservation(String sessionId, Long productId);

      /**
       * セッションの全仮引当を解除する（カートクリア時）
       */
      void releaseAllReservations(String sessionId);

      /**
       * 仮引当を本引当に変換する（注文確定時）
       */
      void commitReservations(String sessionId, Order order);

      /**
       * 本引当を解除する（注文キャンセル時）
       */
      void releaseCommittedReservations(Long orderId);

      /**
       * 在庫調整（差分方式）
       */
      InventoryAdjustment adjustStock(Long productId, Integer quantityDelta, String reason, BoUser admin);

  }
  ```

---

- [ ] **T-7**: InventoryService → InventoryUseCase にリネーム・移動

  ```bash
  cd backend/src/main/java/com/example/aiec/modules/inventory

  # Service → UseCase にリネーム
  git mv domain/service/InventoryService.java application/usecase/InventoryUseCase.java
  ```

  **package宣言の更新**:

  `InventoryUseCase.java` の先頭を以下に変更:

  ```java
  package com.example.aiec.modules.inventory.application.usecase;

  import com.example.aiec.modules.backoffice.domain.entity.BoUser;
  import com.example.aiec.modules.inventory.adapter.dto.AvailabilityDto;
  import com.example.aiec.modules.inventory.adapter.dto.InventoryStatusDto;
  import com.example.aiec.modules.inventory.adapter.dto.ReservationDto;
  import com.example.aiec.modules.inventory.adapter.dto.StockShortageDetail;
  import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
  import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
  import com.example.aiec.modules.inventory.domain.entity.InventoryAdjustment;
  import com.example.aiec.modules.inventory.domain.entity.StockReservation;
  import com.example.aiec.modules.inventory.domain.entity.StockReservation.ReservationType;
  import com.example.aiec.modules.inventory.domain.repository.InventoryAdjustmentRepository;
  import com.example.aiec.modules.inventory.domain.repository.StockReservationRepository;
  import com.example.aiec.modules.product.domain.entity.Product;
  import com.example.aiec.modules.product.domain.repository.ProductRepository;
  import com.example.aiec.modules.purchase.order.entity.Order;
  import com.example.aiec.modules.purchase.order.repository.OrderRepository;
  import com.example.aiec.modules.shared.exception.BusinessException;
  import com.example.aiec.modules.shared.exception.ConflictException;
  import com.example.aiec.modules.shared.exception.InsufficientStockException;
  import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.scheduling.annotation.Scheduled;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Isolation;
  import org.springframework.transaction.annotation.Transactional;

  import java.time.Instant;
  import java.time.temporal.ChronoUnit;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.stream.Collectors;

  /**
   * 在庫ユースケース（Port実装）
   */
  @Service
  @RequiredArgsConstructor
  @Slf4j
  class InventoryUseCase implements InventoryQueryPort, InventoryCommandPort {

      private static final int RESERVATION_EXPIRY_MINUTES = 30;

      private final StockReservationRepository reservationRepository;
      private final ProductRepository productRepository;
      private final OrderRepository orderRepository;
      private final InventoryAdjustmentRepository inventoryAdjustmentRepository;

      // 以下、既存メソッドに @Override を追加
      // reserveTentative メソッドは削除（createReservation と重複）
  }
  ```

  **変更点**:
  - クラス名: `InventoryService` → `InventoryUseCase`
  - `implements InventoryQueryPort, InventoryCommandPort` を追加
  - `@Override` アノテーションを公開メソッドに追加
  - `reserveTentative` メソッドを削除（`createReservation` と重複）
  - クラススコープを `public` → `class`（パッケージプライベート）

---

- [ ] **T-8**: InventoryController の参照をPort経由に変更

  パス: `backend/src/main/java/com/example/aiec/modules/inventory/adapter/rest/InventoryController.java`

  **import文の変更**:

  変更前:
  ```java
  import com.example.aiec.modules.inventory.domain.service.InventoryService;
  ```

  変更後:
  ```java
  import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
  ```

  **フィールド宣言の変更**:

  変更前:
  ```java
  private final InventoryService inventoryService;
  ```

  変更後:
  ```java
  private final InventoryQueryPort inventoryQuery;
  ```

  **メソッド内の変更**:

  全ての `inventoryService.xxx()` 呼び出しを `inventoryQuery.xxx()` に変更

---

- [ ] **T-9**: BoAdminInventoryController の参照をPort経由に変更

  パス: `backend/src/main/java/com/example/aiec/modules/inventory/adapter/rest/BoAdminInventoryController.java`

  **import文の変更**:

  変更前:
  ```java
  import com.example.aiec.modules.inventory.domain.service.InventoryService;
  ```

  変更後:
  ```java
  import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
  import com.example.aiec.modules.inventory.application.port.InventoryQueryPort;
  ```

  **フィールド宣言の変更**:

  変更前:
  ```java
  private final InventoryService inventoryService;
  ```

  変更後:
  ```java
  private final InventoryQueryPort inventoryQuery;
  private final InventoryCommandPort inventoryCommand;
  ```

  **メソッド内の変更**:

  全ての `inventoryService.xxx()` 呼び出しを適切なPort経由に変更

---

- [ ] **T-10**: CartService の InventoryService 参照をPort経由に変更

  パス: `backend/src/main/java/com/example/aiec/modules/purchase/cart/service/CartService.java`

  **import文の変更**:

  変更前:
  ```java
  import com.example.aiec.modules.inventory.domain.service.InventoryService;
  ```

  変更後:
  ```java
  import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
  ```

  **フィールド宣言の変更**:

  変更前:
  ```java
  private final InventoryService inventoryService;
  ```

  変更後:
  ```java
  private final InventoryCommandPort inventoryCommand;
  ```

  **メソッド内の変更**:

  全ての `inventoryService.xxx()` 呼び出しを `inventoryCommand.xxx()` に変更

---

- [ ] **T-11**: OrderService の InventoryService 参照をPort経由に変更

  パス: `backend/src/main/java/com/example/aiec/modules/purchase/order/service/OrderService.java`

  **import文の変更**:

  変更前:
  ```java
  import com.example.aiec.modules.inventory.domain.service.InventoryService;
  ```

  変更後:
  ```java
  import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
  ```

  **フィールド宣言の変更**:

  変更前:
  ```java
  private final InventoryService inventoryService;
  ```

  変更後:
  ```java
  private final InventoryCommandPort inventoryCommand;
  ```

  **メソッド内の変更**:

  全ての `inventoryService.xxx()` 呼び出しを `inventoryCommand.xxx()` に変更

---

### purchase モジュール

- [ ] **T-12**: OrderQueryPort インターフェース作成

  パス: `backend/src/main/java/com/example/aiec/modules/purchase/application/port/OrderQueryPort.java`

  新規ファイル作成:

  ```java
  package com.example.aiec.modules.purchase.application.port;

  import com.example.aiec.modules.purchase.adapter.dto.OrderDto;

  import java.util.List;

  /**
   * 注文クエリAPI（公開インターフェース）
   */
  public interface OrderQueryPort {

      /**
       * 全注文を取得（管理者用）
       */
      List<OrderDto> getAllOrders();

      /**
       * 会員の注文履歴を取得
       */
      List<OrderDto> getOrderHistory(Long userId);

  }
  ```

---

- [ ] **T-13**: OrderCommandPort インターフェース作成

  パス: `backend/src/main/java/com/example/aiec/modules/purchase/application/port/OrderCommandPort.java`

  新規ファイル作成:

  ```java
  package com.example.aiec.modules.purchase.application.port;

  import com.example.aiec.modules.purchase.adapter.dto.OrderDto;

  /**
   * 注文コマンドAPI（公開インターフェース）
   */
  public interface OrderCommandPort {

      /**
       * 注文を確認（PENDING → CONFIRMED）
       */
      OrderDto confirmOrder(Long orderId);

      /**
       * 注文を発送（CONFIRMED → SHIPPED）
       */
      OrderDto shipOrder(Long orderId);

      /**
       * 注文を配達完了（SHIPPED → DELIVERED）
       */
      OrderDto deliverOrder(Long orderId);

  }
  ```

---

- [ ] **T-14**: OrderService → OrderUseCase にリネーム・移動

  ```bash
  cd backend/src/main/java/com/example/aiec/modules/purchase

  # Service → UseCase にリネーム
  git mv order/service/OrderService.java application/usecase/OrderUseCase.java
  ```

  **package宣言の更新**:

  `OrderUseCase.java` の先頭を以下に変更:

  ```java
  package com.example.aiec.modules.purchase.application.usecase;

  import com.example.aiec.modules.customer.domain.entity.User;
  import com.example.aiec.modules.customer.domain.repository.UserRepository;
  import com.example.aiec.modules.inventory.application.port.InventoryCommandPort;
  import com.example.aiec.modules.purchase.adapter.dto.OrderDto;
  import com.example.aiec.modules.purchase.adapter.dto.UnavailableProductDetail;
  import com.example.aiec.modules.purchase.application.port.OrderCommandPort;
  import com.example.aiec.modules.purchase.application.port.OrderQueryPort;
  import com.example.aiec.modules.purchase.cart.entity.Cart;
  import com.example.aiec.modules.purchase.cart.repository.CartRepository;
  import com.example.aiec.modules.purchase.cart.service.CartService;
  import com.example.aiec.modules.purchase.order.entity.Order;
  import com.example.aiec.modules.purchase.order.entity.OrderItem;
  import com.example.aiec.modules.purchase.order.repository.OrderRepository;
  import com.example.aiec.modules.shared.exception.BusinessException;
  import com.example.aiec.modules.shared.exception.ItemNotAvailableException;
  import com.example.aiec.modules.shared.exception.ResourceNotFoundException;
  import lombok.RequiredArgsConstructor;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  import java.math.BigDecimal;
  import java.util.List;

  /**
   * 注文ユースケース（Port実装）
   */
  @Service
  @RequiredArgsConstructor
  class OrderUseCase implements OrderQueryPort, OrderCommandPort {

      private final OrderRepository orderRepository;
      private final CartRepository cartRepository;
      private final CartService cartService;
      private final InventoryCommandPort inventoryCommand;
      private final UserRepository userRepository;

      // 既存の public メソッドのうち、Port に含まれるものに @Override を追加
      // createOrder, cancelOrder, getOrderById は内部メソッドなので Port に含めない
  }
  ```

  **変更点**:
  - クラス名: `OrderService` → `OrderUseCase`
  - `implements OrderQueryPort, OrderCommandPort` を追加
  - `InventoryService inventoryService` → `InventoryCommandPort inventoryCommand`
  - `@Override` アノテーションを公開メソッドに追加
  - クラススコープを `public` → `class`（パッケージプライベート）

---

- [ ] **T-15**: OrderController の参照をUseCase経由に変更

  パス: `backend/src/main/java/com/example/aiec/modules/purchase/adapter/rest/OrderController.java`

  **import文の変更**:

  変更前:
  ```java
  import com.example.aiec.modules.purchase.order.service.OrderService;
  ```

  変更後:
  ```java
  import com.example.aiec.modules.purchase.application.usecase.OrderUseCase;
  import com.example.aiec.modules.purchase.application.port.OrderCommandPort;
  import com.example.aiec.modules.purchase.application.port.OrderQueryPort;
  ```

  **フィールド宣言の変更**:

  変更前:
  ```java
  private final OrderService orderService;
  ```

  変更後:
  ```java
  private final OrderUseCase orderUseCase;  // createOrder 等の内部メソッド用
  private final OrderCommandPort orderCommand;
  private final OrderQueryPort orderQuery;
  ```

  **メソッド内の変更**:

  全ての `orderService.xxx()` 呼び出しを適切な参照先に変更:
  - `createOrder`, `cancelOrder`, `getOrderById` → `orderUseCase.xxx()`
  - `confirmOrder`, `shipOrder`, `deliverOrder` → `orderCommand.xxx()`
  - `getAllOrders`, `getOrderHistory` → `orderQuery.xxx()`

---

- [ ] **T-16**: CartService はそのまま（Port化不要）

  CartService は他モジュールから呼び出されないため、Port化は不要です。
  現在のパッケージ位置（`modules/purchase/cart/service/`）のまま維持します。

---

### customer モジュール

- [ ] **T-17**: AuthService, UserService はそのまま（Port化不要）

  customer モジュールの AuthService, UserService は他モジュールから呼び出されないため、Port化は不要です。
  現在のパッケージ位置（`modules/customer/domain/service/`）のまま維持します。

---

### backoffice モジュール

- [ ] **T-18**: BoAuthService, BoUserService はそのまま（Port化不要）

  backoffice モジュールの BoAuthService, BoUserService は他モジュールから呼び出されないため、Port化は不要です。
  現在のパッケージ位置（`modules/backoffice/domain/service/`）のまま維持します。

---

### shared モジュール

- [ ] **T-19**: OperationHistoryService はそのまま（Port化不要）

  OperationHistoryService は Phase 4（イベント実装）で刷新するため、現時点では Port化不要です。
  現在のパッケージ位置（`modules/shared/domain/service/`）のまま維持します。

---

### 統合作業

- [ ] **T-20**: コンパイル確認とエラー修正

  ```bash
  docker compose exec backend ./mvnw compile
  ```

  コンパイルエラーが出た場合:
  1. エラーメッセージからファイルとimport文を特定
  2. 該当ファイルのimport文を正しいモジュールパスに修正
  3. 再度コンパイル

  **よくあるエラー**:
  - `cannot find symbol`: Port インターフェースのimportが不足
  - `incompatible types`: Port 型への変更漏れ

---

- [ ] **T-21**: 既存テストの実行確認

  ```bash
  docker compose exec backend ./mvnw test
  ```

  全テストがパスすることを確認。失敗した場合は該当テストファイルのimport文を修正。

---

## 実装順序

```
T-0（ディレクトリ作成）
  → T-1, T-2, T-3（product Port作成・UseCase移行）
    → T-4（ProductController修正）
      → T-5, T-6, T-7（inventory Port作成・UseCase移行）
        → T-8, T-9, T-10, T-11（inventory参照の修正）
          → T-12, T-13, T-14（purchase Port作成・UseCase移行）
            → T-15（OrderController修正）
              → T-16, T-17, T-18, T-19（その他確認）
                → T-20（コンパイル確認）
                  → T-21（テスト実行確認）
```

**注意**: 各モジュールの Port 作成 → UseCase 移行 → 参照修正を順次実施すること。

---

## テスト手順

Phase 2完了後に以下を確認:

1. `docker compose exec backend ./mvnw compile` → コンパイル成功
2. `docker compose exec backend ./mvnw test` → 既存テスト全パス
3. 各モジュールの `application/port/` パッケージに公開インターフェースが存在
4. 各モジュールの `application/usecase/` パッケージに実装クラスが存在
5. 他モジュールからの参照がPort経由になっている（直接 domain.service を参照していない）
6. Controller からの参照がPort経由になっている

---

## Phase 3 への準備

Phase 2完了後、以下を実施予定:
- **Phase 3**: ArchUnit導入（境界制約のテスト追加）

Phase 3の詳細は別途タスクファイルを参照。
