# CHG-013: モジュラーモノリス設計 Phase 1 - 実装タスク

要件: `docs/01_requirements/CHG-013_モジュラーモノリス設計.md`
設計: `docs/02_designs/CHG-013_モジュラーモノリス設計.md`
作成日: 2026-02-18

検証コマンド:
- バックエンド: `docker compose exec backend ./mvnw compile`
- 全テスト: `docker compose exec backend ./mvnw test`
- コンテナ未起動の場合: `docker compose up -d` を先に実行

---

## Phase 1 概要

Phase 1では、既存の横割り構造（controller/service/repository/entity）を、モジュラーモノリスのパッケージ構造に移行します。

**対象モジュール**: product / inventory / purchase / customer / backoffice / shared

**移行方針**:
1. `git mv` でファイルを移動（履歴を保持）
2. 各ファイルのpackage宣言を更新
3. import文を一括置換
4. コンパイルエラーがなくなるまで調整
5. 既存テストが全パス

---

## タスク一覧

### 準備

- [ ] **T-0**: モジュール用ディレクトリ構造の作成

  以下のコマンドで、全モジュールのディレクトリ構造を作成:

  ```bash
  cd backend/src/main/java/com/example/aiec

  # product モジュール
  mkdir -p modules/product/domain/entity
  mkdir -p modules/product/domain/repository
  mkdir -p modules/product/domain/service
  mkdir -p modules/product/adapter/rest
  mkdir -p modules/product/adapter/dto

  # inventory モジュール
  mkdir -p modules/inventory/domain/entity
  mkdir -p modules/inventory/domain/repository
  mkdir -p modules/inventory/domain/service
  mkdir -p modules/inventory/adapter/rest
  mkdir -p modules/inventory/adapter/dto

  # purchase モジュール
  mkdir -p modules/purchase/cart/entity
  mkdir -p modules/purchase/cart/repository
  mkdir -p modules/purchase/cart/service
  mkdir -p modules/purchase/order/entity
  mkdir -p modules/purchase/order/repository
  mkdir -p modules/purchase/order/service
  mkdir -p modules/purchase/adapter/rest
  mkdir -p modules/purchase/adapter/dto

  # customer モジュール
  mkdir -p modules/customer/domain/entity
  mkdir -p modules/customer/domain/repository
  mkdir -p modules/customer/domain/service
  mkdir -p modules/customer/adapter/rest
  mkdir -p modules/customer/adapter/dto

  # backoffice モジュール
  mkdir -p modules/backoffice/domain/entity
  mkdir -p modules/backoffice/domain/repository
  mkdir -p modules/backoffice/domain/service
  mkdir -p modules/backoffice/adapter/rest
  mkdir -p modules/backoffice/adapter/dto

  # shared モジュール
  mkdir -p modules/shared/domain/entity
  mkdir -p modules/shared/domain/repository
  mkdir -p modules/shared/domain/service
  mkdir -p modules/shared/domain/model
  mkdir -p modules/shared/dto
  mkdir -p modules/shared/exception
  ```

---

### product モジュール

- [ ] **T-1**: product モジュールへのファイル移動

  ```bash
  cd backend/src/main/java/com/example/aiec

  # Entity移動
  git mv entity/Product.java modules/product/domain/entity/

  # Repository移動
  git mv repository/ProductRepository.java modules/product/domain/repository/

  # Service移動
  git mv service/ProductService.java modules/product/domain/service/

  # Controller移動
  git mv controller/ItemController.java modules/product/adapter/rest/ProductController.java

  # DTO移動
  git mv dto/ProductDto.java modules/product/adapter/dto/
  git mv dto/ProductListResponse.java modules/product/adapter/dto/
  git mv dto/UpdateProductRequest.java modules/product/adapter/dto/
  ```

  **package宣言の更新**:

  各ファイルの先頭のpackage宣言を以下のように更新:

  | ファイル | 変更前 | 変更後 |
  |---------|--------|--------|
  | Product.java | `package com.example.aiec.entity;` | `package com.example.aiec.modules.product.domain.entity;` |
  | ProductRepository.java | `package com.example.aiec.repository;` | `package com.example.aiec.modules.product.domain.repository;` |
  | ProductService.java | `package com.example.aiec.service;` | `package com.example.aiec.modules.product.domain.service;` |
  | ProductController.java | `package com.example.aiec.controller;` | `package com.example.aiec.modules.product.adapter.rest;` |
  | ProductDto.java | `package com.example.aiec.dto;` | `package com.example.aiec.modules.product.adapter.dto;` |
  | ProductListResponse.java | `package com.example.aiec.dto;` | `package com.example.aiec.modules.product.adapter.dto;` |
  | UpdateProductRequest.java | `package com.example.aiec.dto;` | `package com.example.aiec.modules.product.adapter.dto;` |

  **import文の更新**:

  各ファイル内で以下のimportパターンを一括置換:

  ```
  import com.example.aiec.entity.Product;
    → import com.example.aiec.modules.product.domain.entity.Product;

  import com.example.aiec.repository.ProductRepository;
    → import com.example.aiec.modules.product.domain.repository.ProductRepository;

  import com.example.aiec.service.ProductService;
    → import com.example.aiec.modules.product.domain.service.ProductService;

  import com.example.aiec.dto.ProductDto;
    → import com.example.aiec.modules.product.adapter.dto.ProductDto;

  import com.example.aiec.dto.ProductListResponse;
    → import com.example.aiec.modules.product.adapter.dto.ProductListResponse;

  import com.example.aiec.dto.UpdateProductRequest;
    → import com.example.aiec.modules.product.adapter.dto.UpdateProductRequest;
  ```

---

### inventory モジュール

- [ ] **T-2**: inventory モジュールへのファイル移動

  ```bash
  cd backend/src/main/java/com/example/aiec

  # Entity移動
  git mv entity/StockReservation.java modules/inventory/domain/entity/
  git mv entity/InventoryAdjustment.java modules/inventory/domain/entity/

  # Repository移動
  git mv repository/StockReservationRepository.java modules/inventory/domain/repository/
  git mv repository/InventoryAdjustmentRepository.java modules/inventory/domain/repository/

  # Service移動
  git mv service/InventoryService.java modules/inventory/domain/service/

  # Controller移動
  git mv controller/InventoryController.java modules/inventory/adapter/rest/
  git mv controller/BoAdminInventoryController.java modules/inventory/adapter/rest/

  # DTO移動
  git mv dto/InventoryStatusDto.java modules/inventory/adapter/dto/
  git mv dto/StockShortageDetail.java modules/inventory/adapter/dto/
  git mv dto/ReservationDto.java modules/inventory/adapter/dto/
  git mv dto/CreateReservationRequest.java modules/inventory/adapter/dto/
  git mv dto/AvailabilityDto.java modules/inventory/adapter/dto/
  ```

  **package宣言の更新**:

  | ファイル | 変更後 |
  |---------|--------|
  | StockReservation.java | `package com.example.aiec.modules.inventory.domain.entity;` |
  | InventoryAdjustment.java | `package com.example.aiec.modules.inventory.domain.entity;` |
  | StockReservationRepository.java | `package com.example.aiec.modules.inventory.domain.repository;` |
  | InventoryAdjustmentRepository.java | `package com.example.aiec.modules.inventory.domain.repository;` |
  | InventoryService.java | `package com.example.aiec.modules.inventory.domain.service;` |
  | InventoryController.java | `package com.example.aiec.modules.inventory.adapter.rest;` |
  | BoAdminInventoryController.java | `package com.example.aiec.modules.inventory.adapter.rest;` |
  | InventoryStatusDto.java | `package com.example.aiec.modules.inventory.adapter.dto;` |
  | StockShortageDetail.java | `package com.example.aiec.modules.inventory.adapter.dto;` |
  | ReservationDto.java | `package com.example.aiec.modules.inventory.adapter.dto;` |
  | CreateReservationRequest.java | `package com.example.aiec.modules.inventory.adapter.dto;` |
  | AvailabilityDto.java | `package com.example.aiec.modules.inventory.adapter.dto;` |

  **import文の更新**:

  各ファイル内で以下のimportパターンを一括置換:

  ```
  import com.example.aiec.entity.StockReservation;
    → import com.example.aiec.modules.inventory.domain.entity.StockReservation;

  import com.example.aiec.entity.InventoryAdjustment;
    → import com.example.aiec.modules.inventory.domain.entity.InventoryAdjustment;

  import com.example.aiec.repository.StockReservationRepository;
    → import com.example.aiec.modules.inventory.domain.repository.StockReservationRepository;

  import com.example.aiec.repository.InventoryAdjustmentRepository;
    → import com.example.aiec.modules.inventory.domain.repository.InventoryAdjustmentRepository;

  import com.example.aiec.service.InventoryService;
    → import com.example.aiec.modules.inventory.domain.service.InventoryService;

  import com.example.aiec.dto.InventoryStatusDto;
    → import com.example.aiec.modules.inventory.adapter.dto.InventoryStatusDto;

  import com.example.aiec.dto.StockShortageDetail;
    → import com.example.aiec.modules.inventory.adapter.dto.StockShortageDetail;

  import com.example.aiec.dto.ReservationDto;
    → import com.example.aiec.modules.inventory.adapter.dto.ReservationDto;

  import com.example.aiec.dto.CreateReservationRequest;
    → import com.example.aiec.modules.inventory.adapter.dto.CreateReservationRequest;

  import com.example.aiec.dto.AvailabilityDto;
    → import com.example.aiec.modules.inventory.adapter.dto.AvailabilityDto;
  ```

---

### purchase モジュール

- [ ] **T-3**: purchase モジュールへのファイル移動

  ```bash
  cd backend/src/main/java/com/example/aiec

  # Cart Entity移動
  git mv entity/Cart.java modules/purchase/cart/entity/
  git mv entity/CartItem.java modules/purchase/cart/entity/

  # Cart Repository移動
  git mv repository/CartRepository.java modules/purchase/cart/repository/
  git mv repository/CartItemRepository.java modules/purchase/cart/repository/

  # Cart Service移動
  git mv service/CartService.java modules/purchase/cart/service/

  # Order Entity移動
  git mv entity/Order.java modules/purchase/order/entity/
  git mv entity/OrderItem.java modules/purchase/order/entity/

  # Order Repository移動
  git mv repository/OrderRepository.java modules/purchase/order/repository/
  git mv repository/OrderItemRepository.java modules/purchase/order/repository/

  # Order Service移動
  git mv service/OrderService.java modules/purchase/order/service/

  # Controller移動
  git mv controller/OrderController.java modules/purchase/adapter/rest/

  # DTO移動
  git mv dto/CartDto.java modules/purchase/adapter/dto/
  git mv dto/CartItemDto.java modules/purchase/adapter/dto/
  git mv dto/AddToCartRequest.java modules/purchase/adapter/dto/
  git mv dto/UpdateQuantityRequest.java modules/purchase/adapter/dto/
  git mv dto/OrderDto.java modules/purchase/adapter/dto/
  git mv dto/OrderItemDto.java modules/purchase/adapter/dto/
  git mv dto/CreateOrderRequest.java modules/purchase/adapter/dto/
  git mv dto/UnavailableProductDetail.java modules/purchase/adapter/dto/
  ```

  **package宣言の更新**:

  | ファイル | 変更後 |
  |---------|--------|
  | Cart.java | `package com.example.aiec.modules.purchase.cart.entity;` |
  | CartItem.java | `package com.example.aiec.modules.purchase.cart.entity;` |
  | CartRepository.java | `package com.example.aiec.modules.purchase.cart.repository;` |
  | CartItemRepository.java | `package com.example.aiec.modules.purchase.cart.repository;` |
  | CartService.java | `package com.example.aiec.modules.purchase.cart.service;` |
  | Order.java | `package com.example.aiec.modules.purchase.order.entity;` |
  | OrderItem.java | `package com.example.aiec.modules.purchase.order.entity;` |
  | OrderRepository.java | `package com.example.aiec.modules.purchase.order.repository;` |
  | OrderItemRepository.java | `package com.example.aiec.modules.purchase.order.repository;` |
  | OrderService.java | `package com.example.aiec.modules.purchase.order.service;` |
  | OrderController.java | `package com.example.aiec.modules.purchase.adapter.rest;` |
  | CartDto.java | `package com.example.aiec.modules.purchase.adapter.dto;` |
  | CartItemDto.java | `package com.example.aiec.modules.purchase.adapter.dto;` |
  | AddToCartRequest.java | `package com.example.aiec.modules.purchase.adapter.dto;` |
  | UpdateQuantityRequest.java | `package com.example.aiec.modules.purchase.adapter.dto;` |
  | OrderDto.java | `package com.example.aiec.modules.purchase.adapter.dto;` |
  | OrderItemDto.java | `package com.example.aiec.modules.purchase.adapter.dto;` |
  | CreateOrderRequest.java | `package com.example.aiec.modules.purchase.adapter.dto;` |
  | UnavailableProductDetail.java | `package com.example.aiec.modules.purchase.adapter.dto;` |

  **import文の更新**:

  各ファイル内で以下のimportパターンを一括置換:

  ```
  import com.example.aiec.entity.Cart;
    → import com.example.aiec.modules.purchase.cart.entity.Cart;

  import com.example.aiec.entity.CartItem;
    → import com.example.aiec.modules.purchase.cart.entity.CartItem;

  import com.example.aiec.repository.CartRepository;
    → import com.example.aiec.modules.purchase.cart.repository.CartRepository;

  import com.example.aiec.repository.CartItemRepository;
    → import com.example.aiec.modules.purchase.cart.repository.CartItemRepository;

  import com.example.aiec.service.CartService;
    → import com.example.aiec.modules.purchase.cart.service.CartService;

  import com.example.aiec.entity.Order;
    → import com.example.aiec.modules.purchase.order.entity.Order;

  import com.example.aiec.entity.OrderItem;
    → import com.example.aiec.modules.purchase.order.entity.OrderItem;

  import com.example.aiec.repository.OrderRepository;
    → import com.example.aiec.modules.purchase.order.repository.OrderRepository;

  import com.example.aiec.repository.OrderItemRepository;
    → import com.example.aiec.modules.purchase.order.repository.OrderItemRepository;

  import com.example.aiec.service.OrderService;
    → import com.example.aiec.modules.purchase.order.service.OrderService;

  import com.example.aiec.dto.CartDto;
    → import com.example.aiec.modules.purchase.adapter.dto.CartDto;

  import com.example.aiec.dto.CartItemDto;
    → import com.example.aiec.modules.purchase.adapter.dto.CartItemDto;

  import com.example.aiec.dto.AddToCartRequest;
    → import com.example.aiec.modules.purchase.adapter.dto.AddToCartRequest;

  import com.example.aiec.dto.UpdateQuantityRequest;
    → import com.example.aiec.modules.purchase.adapter.dto.UpdateQuantityRequest;

  import com.example.aiec.dto.OrderDto;
    → import com.example.aiec.modules.purchase.adapter.dto.OrderDto;

  import com.example.aiec.dto.OrderItemDto;
    → import com.example.aiec.modules.purchase.adapter.dto.OrderItemDto;

  import com.example.aiec.dto.CreateOrderRequest;
    → import com.example.aiec.modules.purchase.adapter.dto.CreateOrderRequest;

  import com.example.aiec.dto.UnavailableProductDetail;
    → import com.example.aiec.modules.purchase.adapter.dto.UnavailableProductDetail;
  ```

---

### customer モジュール

- [ ] **T-4**: customer モジュールへのファイル移動

  ```bash
  cd backend/src/main/java/com/example/aiec

  # Entity移動
  git mv entity/User.java modules/customer/domain/entity/
  git mv entity/AuthToken.java modules/customer/domain/entity/

  # Repository移動
  git mv repository/UserRepository.java modules/customer/domain/repository/
  git mv repository/AuthTokenRepository.java modules/customer/domain/repository/

  # Service移動
  git mv service/UserService.java modules/customer/domain/service/
  git mv service/AuthService.java modules/customer/domain/service/

  # Controller移動
  git mv controller/AuthController.java modules/customer/adapter/rest/

  # DTO移動
  git mv dto/UserDto.java modules/customer/adapter/dto/
  git mv dto/AuthResponse.java modules/customer/adapter/dto/
  git mv dto/LoginRequest.java modules/customer/adapter/dto/
  git mv dto/RegisterRequest.java modules/customer/adapter/dto/
  ```

  **package宣言の更新**:

  | ファイル | 変更後 |
  |---------|--------|
  | User.java | `package com.example.aiec.modules.customer.domain.entity;` |
  | AuthToken.java | `package com.example.aiec.modules.customer.domain.entity;` |
  | UserRepository.java | `package com.example.aiec.modules.customer.domain.repository;` |
  | AuthTokenRepository.java | `package com.example.aiec.modules.customer.domain.repository;` |
  | UserService.java | `package com.example.aiec.modules.customer.domain.service;` |
  | AuthService.java | `package com.example.aiec.modules.customer.domain.service;` |
  | AuthController.java | `package com.example.aiec.modules.customer.adapter.rest;` |
  | UserDto.java | `package com.example.aiec.modules.customer.adapter.dto;` |
  | AuthResponse.java | `package com.example.aiec.modules.customer.adapter.dto;` |
  | LoginRequest.java | `package com.example.aiec.modules.customer.adapter.dto;` |
  | RegisterRequest.java | `package com.example.aiec.modules.customer.adapter.dto;` |

  **import文の更新**:

  各ファイル内で以下のimportパターンを一括置換:

  ```
  import com.example.aiec.entity.User;
    → import com.example.aiec.modules.customer.domain.entity.User;

  import com.example.aiec.entity.AuthToken;
    → import com.example.aiec.modules.customer.domain.entity.AuthToken;

  import com.example.aiec.repository.UserRepository;
    → import com.example.aiec.modules.customer.domain.repository.UserRepository;

  import com.example.aiec.repository.AuthTokenRepository;
    → import com.example.aiec.modules.customer.domain.repository.AuthTokenRepository;

  import com.example.aiec.service.UserService;
    → import com.example.aiec.modules.customer.domain.service.UserService;

  import com.example.aiec.service.AuthService;
    → import com.example.aiec.modules.customer.domain.service.AuthService;

  import com.example.aiec.dto.UserDto;
    → import com.example.aiec.modules.customer.adapter.dto.UserDto;

  import com.example.aiec.dto.AuthResponse;
    → import com.example.aiec.modules.customer.adapter.dto.AuthResponse;

  import com.example.aiec.dto.LoginRequest;
    → import com.example.aiec.modules.customer.adapter.dto.LoginRequest;

  import com.example.aiec.dto.RegisterRequest;
    → import com.example.aiec.modules.customer.adapter.dto.RegisterRequest;
  ```

---

### backoffice モジュール

- [ ] **T-5**: backoffice モジュールへのファイル移動

  ```bash
  cd backend/src/main/java/com/example/aiec

  # Entity移動
  git mv entity/BoUser.java modules/backoffice/domain/entity/
  git mv entity/BoAuthToken.java modules/backoffice/domain/entity/

  # Repository移動
  git mv repository/BoUserRepository.java modules/backoffice/domain/repository/
  git mv repository/BoAuthTokenRepository.java modules/backoffice/domain/repository/

  # Service移動
  git mv service/BoUserService.java modules/backoffice/domain/service/
  git mv service/BoAuthService.java modules/backoffice/domain/service/

  # Controller移動
  git mv controller/BoAuthController.java modules/backoffice/adapter/rest/
  git mv controller/BoAdminController.java modules/backoffice/adapter/rest/
  git mv controller/BoAdminBoUsersController.java modules/backoffice/adapter/rest/

  # DTO移動
  git mv dto/BoUserDto.java modules/backoffice/adapter/dto/
  git mv dto/BoAuthResponse.java modules/backoffice/adapter/dto/
  git mv dto/BoLoginRequest.java modules/backoffice/adapter/dto/
  git mv dto/MemberDetailDto.java modules/backoffice/adapter/dto/
  ```

  **package宣言の更新**:

  | ファイル | 変更後 |
  |---------|--------|
  | BoUser.java | `package com.example.aiec.modules.backoffice.domain.entity;` |
  | BoAuthToken.java | `package com.example.aiec.modules.backoffice.domain.entity;` |
  | BoUserRepository.java | `package com.example.aiec.modules.backoffice.domain.repository;` |
  | BoAuthTokenRepository.java | `package com.example.aiec.modules.backoffice.domain.repository;` |
  | BoUserService.java | `package com.example.aiec.modules.backoffice.domain.service;` |
  | BoAuthService.java | `package com.example.aiec.modules.backoffice.domain.service;` |
  | BoAuthController.java | `package com.example.aiec.modules.backoffice.adapter.rest;` |
  | BoAdminController.java | `package com.example.aiec.modules.backoffice.adapter.rest;` |
  | BoAdminBoUsersController.java | `package com.example.aiec.modules.backoffice.adapter.rest;` |
  | BoUserDto.java | `package com.example.aiec.modules.backoffice.adapter.dto;` |
  | BoAuthResponse.java | `package com.example.aiec.modules.backoffice.adapter.dto;` |
  | BoLoginRequest.java | `package com.example.aiec.modules.backoffice.adapter.dto;` |
  | MemberDetailDto.java | `package com.example.aiec.modules.backoffice.adapter.dto;` |

  **import文の更新**:

  各ファイル内で以下のimportパターンを一括置換:

  ```
  import com.example.aiec.entity.BoUser;
    → import com.example.aiec.modules.backoffice.domain.entity.BoUser;

  import com.example.aiec.entity.BoAuthToken;
    → import com.example.aiec.modules.backoffice.domain.entity.BoAuthToken;

  import com.example.aiec.repository.BoUserRepository;
    → import com.example.aiec.modules.backoffice.domain.repository.BoUserRepository;

  import com.example.aiec.repository.BoAuthTokenRepository;
    → import com.example.aiec.modules.backoffice.domain.repository.BoAuthTokenRepository;

  import com.example.aiec.service.BoUserService;
    → import com.example.aiec.modules.backoffice.domain.service.BoUserService;

  import com.example.aiec.service.BoAuthService;
    → import com.example.aiec.modules.backoffice.domain.service.BoAuthService;

  import com.example.aiec.dto.BoUserDto;
    → import com.example.aiec.modules.backoffice.adapter.dto.BoUserDto;

  import com.example.aiec.dto.BoAuthResponse;
    → import com.example.aiec.modules.backoffice.adapter.dto.BoAuthResponse;

  import com.example.aiec.dto.BoLoginRequest;
    → import com.example.aiec.modules.backoffice.adapter.dto.BoLoginRequest;

  import com.example.aiec.dto.MemberDetailDto;
    → import com.example.aiec.modules.backoffice.adapter.dto.MemberDetailDto;
  ```

---

### shared モジュール

- [ ] **T-6**: shared モジュールへのファイル移動

  ```bash
  cd backend/src/main/java/com/example/aiec

  # Enum/Model移動
  git mv entity/ActorType.java modules/shared/domain/model/
  git mv entity/PermissionLevel.java modules/shared/domain/model/

  # Entity移動
  git mv entity/OperationHistory.java modules/shared/domain/entity/

  # Repository移動
  git mv repository/OperationHistoryRepository.java modules/shared/domain/repository/

  # Service移動
  git mv service/OperationHistoryService.java modules/shared/domain/service/

  # Exception移動（全ファイル）
  git mv exception/* modules/shared/exception/

  # 共通DTO移動
  git mv dto/ApiResponse.java modules/shared/dto/
  ```

  **package宣言の更新**:

  | ファイル | 変更後 |
  |---------|--------|
  | ActorType.java | `package com.example.aiec.modules.shared.domain.model;` |
  | PermissionLevel.java | `package com.example.aiec.modules.shared.domain.model;` |
  | OperationHistory.java | `package com.example.aiec.modules.shared.domain.entity;` |
  | OperationHistoryRepository.java | `package com.example.aiec.modules.shared.domain.repository;` |
  | OperationHistoryService.java | `package com.example.aiec.modules.shared.domain.service;` |
  | exception配下の全クラス | `package com.example.aiec.modules.shared.exception;` |
  | ApiResponse.java | `package com.example.aiec.modules.shared.dto;` |

  **import文の更新**:

  各ファイル内で以下のimportパターンを一括置換:

  ```
  import com.example.aiec.entity.ActorType;
    → import com.example.aiec.modules.shared.domain.model.ActorType;

  import com.example.aiec.entity.PermissionLevel;
    → import com.example.aiec.modules.shared.domain.model.PermissionLevel;

  import com.example.aiec.entity.OperationHistory;
    → import com.example.aiec.modules.shared.domain.entity.OperationHistory;

  import com.example.aiec.repository.OperationHistoryRepository;
    → import com.example.aiec.modules.shared.domain.repository.OperationHistoryRepository;

  import com.example.aiec.service.OperationHistoryService;
    → import com.example.aiec.modules.shared.domain.service.OperationHistoryService;

  import com.example.aiec.exception.
    → import com.example.aiec.modules.shared.exception.

  import com.example.aiec.dto.ApiResponse;
    → import com.example.aiec.modules.shared.dto.ApiResponse;
  ```

---

### 統合作業

- [ ] **T-7**: 全ファイルのimport一括置換スクリプト実行

  全Javaファイルで一括置換を実行:

  ```bash
  cd backend/src/main/java/com/example/aiec/modules

  # 全ファイルに対してimport文を一括置換
  find . -name "*.java" -type f -exec sed -i '' \
    -e 's|com\.example\.aiec\.entity\.|com.example.aiec.modules.|g' \
    -e 's|com\.example\.aiec\.repository\.|com.example.aiec.modules.|g' \
    -e 's|com\.example\.aiec\.service\.|com.example.aiec.modules.|g' \
    -e 's|com\.example\.aiec\.controller\.|com.example.aiec.modules.|g' \
    -e 's|com\.example\.aiec\.dto\.|com.example.aiec.modules.|g' \
    -e 's|com\.example\.aiec\.exception\.|com.example.aiec.modules.shared.exception.|g' \
    {} \;

  # 個別の置換が必要な場合は手動で調整
  ```

  **重要**: この置換は大まかなパターンマッチなので、以下を手動確認:
  - product モジュールの `Product` は `modules.product.domain.entity.Product`
  - inventory モジュールの `StockReservation` は `modules.inventory.domain.entity.StockReservation`
  - purchase モジュールの `Cart` は `modules.purchase.cart.entity.Cart`
  - purchase モジュールの `Order` は `modules.purchase.order.entity.Order`
  - customer モジュールの `User` は `modules.customer.domain.entity.User`
  - backoffice モジュールの `BoUser` は `modules.backoffice.domain.entity.BoUser`
  - shared モジュールの `ActorType` は `modules.shared.domain.model.ActorType`

---

- [ ] **T-8**: TestController の移動

  ```bash
  cd backend/src/main/java/com/example/aiec

  # TestController は config ディレクトリに移動（グローバル設定として扱う）
  git mv controller/TestController.java config/
  ```

  **package宣言の更新**:

  `TestController.java` の先頭:
  ```java
  package com.example.aiec.config;
  ```

  **import文の更新**:
  ```
  import com.example.aiec.dto.ApiResponse;
    → import com.example.aiec.modules.shared.dto.ApiResponse;
  ```

---

- [ ] **T-9**: 空ディレクトリの削除

  ```bash
  cd backend/src/main/java/com/example/aiec

  # 既存の空ディレクトリを削除
  rmdir entity repository service controller dto exception 2>/dev/null || true
  ```

  ※ ディレクトリが空でない場合は削除されません。残っている場合は手動確認して削除。

---

- [ ] **T-10**: コンパイル確認とエラー修正

  ```bash
  docker compose exec backend ./mvnw compile
  ```

  コンパイルエラーが出た場合:
  1. エラーメッセージからファイルとimport文を特定
  2. 該当ファイルのimport文を正しいモジュールパスに修正
  3. 再度コンパイル

  **よくあるエラー**:
  - `cannot find symbol`: import文のパスが間違っている
  - `package does not exist`: package宣言とディレクトリ構造が一致していない

---

- [ ] **T-11**: 既存テストの実行確認

  ```bash
  docker compose exec backend ./mvnw test
  ```

  全テストがパスすることを確認。失敗した場合は該当テストファイルのimport文を修正。

---

## 実装順序

```
T-0（ディレクトリ作成）
  → T-1（product）, T-2（inventory）, T-3（purchase）, T-4（customer）, T-5（backoffice）, T-6（shared）（並行可能）
    → T-7（import一括置換）
      → T-8（TestController移動）
        → T-9（空ディレクトリ削除）
          → T-10（コンパイル確認）
            → T-11（テスト実行確認）
```

**注意**: T-1〜T-6は独立しているため並行実装可能ですが、各タスク内では以下の順序を守ること:
1. ファイル移動（`git mv`）
2. package宣言の更新
3. import文の更新

---

## テスト手順

Phase 1完了後に以下を確認:

1. `docker compose exec backend ./mvnw compile` → コンパイル成功
2. `docker compose exec backend ./mvnw test` → 既存テスト全パス
3. `git status` → 移動が正しく反映されている（Added/Deleted ではなく Renamed として表示）
4. パッケージエクスプローラーで `backend/src/main/java/com/example/aiec/modules/` 配下に各モジュールが存在
5. 旧ディレクトリ（`entity`, `repository`, `service`, `controller`, `dto`, `exception`）が存在しない

---

## Phase 2 以降への準備

Phase 1完了後、以下を実施予定:
- **Phase 2**: Port抽出（`application.port` パッケージの作成、Service → Port + UseCase 分離）
- **Phase 3**: ArchUnit導入（境界制約のテスト追加）
- **Phase 4**: 監査ログイベント実装（`OperationPerformedEvent`）

Phase 2以降の詳細は別途タスクファイルを作成予定。
