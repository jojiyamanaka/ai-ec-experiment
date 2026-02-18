# CHG-012: API契約管理 - 実装タスク

要件: `docs/01_requirements/CHG-012_API契約管理.md`
設計: `docs/02_designs/CHG-012_API契約管理.md`
作成日: 2026-02-18

検証コマンド:
- バックエンド: `docker compose exec backend ./mvnw compile`
- 全テスト: `docker compose exec backend ./mvnw test`
- OpenAPI JSON 生成: `docker compose exec backend ./mvnw verify -DskipTests`
- コンテナ未起動の場合: `docker compose up -d` を先に実行

---

## タスク一覧

### バックエンド

- [ ] **T-1**: pom.xml に springdoc-openapi 依存と maven-plugin を追加

  パス: `backend/pom.xml`

  **変更1: dependencies セクション末尾に追加（94行目 `</dependencies>` の直前）**:

  ```xml
        <!-- OpenAPI / Swagger UI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.8.4</version>
        </dependency>
  ```

  **変更2: plugins セクション末尾に追加（129行目 `</plugins>` の直前）**:

  ```xml
            <plugin>
                <groupId>org.springdoc</groupId>
                <artifactId>springdoc-openapi-maven-plugin</artifactId>
                <version>1.4</version>
                <executions>
                    <execution>
                        <id>generate-openapi</id>
                        <phase>integration-test</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <apiDocsUrl>http://localhost:8080/v3/api-docs</apiDocsUrl>
                    <outputFileName>openapi.json</outputFileName>
                    <outputDir>${project.build.directory}</outputDir>
                </configuration>
            </plugin>
  ```

---

- [ ] **T-2**: application.yml に springdoc 設定を追加

  パス: `backend/src/main/resources/application.yml`

  **挿入位置: 59行目（`allow-credentials: true` の後、`---` の前）に追加**:

  ```yaml

  # OpenAPI / Swagger UI 設定
  springdoc:
    api-docs:
      path: /v3/api-docs
    swagger-ui:
      path: /swagger-ui.html
      tags-sorter: alpha
      operations-sorter: method
    default-produces-media-type: application/json
    default-consumes-media-type: application/json
  ```

---

- [ ] **T-3**: OpenApiConfig.java を新規作成

  パス: `backend/src/main/java/com/example/aiec/config/OpenApiConfig.java`（新規作成）

  ```java
  package com.example.aiec.config;

  import io.swagger.v3.oas.models.OpenAPI;
  import io.swagger.v3.oas.models.info.Info;
  import io.swagger.v3.oas.models.security.SecurityRequirement;
  import io.swagger.v3.oas.models.security.SecurityScheme;
  import org.springframework.context.annotation.Bean;
  import org.springframework.context.annotation.Configuration;

  @Configuration
  public class OpenApiConfig {

      @Bean
      public OpenAPI openAPI() {
          return new OpenAPI()
                  .info(new Info()
                          .title("AI EC Core API")
                          .version("1.0.0")
                          .description("AI EC Experiment の Core API 仕様"))
                  .addSecurityItem(new SecurityRequirement()
                          .addList("SessionId")
                          .addList("BearerAuth")
                          .addList("BoAuth"))
                  .schemaRequirement("SessionId", new SecurityScheme()
                          .type(SecurityScheme.Type.APIKEY)
                          .in(SecurityScheme.In.HEADER)
                          .name("X-Session-Id")
                          .description("セッションID（カート・注文操作に必要）"))
                  .schemaRequirement("BearerAuth", new SecurityScheme()
                          .type(SecurityScheme.Type.HTTP)
                          .scheme("bearer")
                          .description("顧客認証トークン"))
                  .schemaRequirement("BoAuth", new SecurityScheme()
                          .type(SecurityScheme.Type.HTTP)
                          .scheme("bearer")
                          .description("管理者認証トークン"));
      }
  }
  ```

---

- [ ] **T-4**: ItemController に @Tag, @Operation, @Parameter を追加

  パス: `backend/src/main/java/com/example/aiec/controller/ItemController.java`

  **変更前（1-4行目）**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.ApiResponse;
  import com.example.aiec.dto.ProductDto;
  ```

  **変更後**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.ApiResponse;
  import com.example.aiec.dto.ProductDto;
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.Parameter;
  import io.swagger.v3.oas.annotations.tags.Tag;
  ```

  **変更前（21-24行目）**:

  ```java
  @RestController
  @RequestMapping("/api/item")
  @RequiredArgsConstructor
  public class ItemController {
  ```

  **変更後**:

  ```java
  @RestController
  @RequestMapping("/api/item")
  @RequiredArgsConstructor
  @Tag(name = "商品", description = "商品の取得・更新")
  public class ItemController {
  ```

  **変更前（34行目）**:

  ```java
      @GetMapping
  ```

  **変更後**:

  ```java
      @GetMapping
      @Operation(summary = "商品一覧取得", description = "公開商品の一覧をページネーション付きで取得")
  ```

  **変更前（36-37行目）**:

  ```java
              @RequestParam(defaultValue = "1") int page,
              @RequestParam(defaultValue = "20") int limit
  ```

  **変更後**:

  ```java
              @RequestParam(defaultValue = "1") @Parameter(description = "ページ番号") int page,
              @RequestParam(defaultValue = "20") @Parameter(description = "1ページあたりの件数") int limit
  ```

  **変更前（47行目）**:

  ```java
      @GetMapping("/{id}")
  ```

  **変更後**:

  ```java
      @GetMapping("/{id}")
      @Operation(summary = "商品詳細取得", description = "指定IDの商品情報を取得")
  ```

  **変更前（57行目）**:

  ```java
      @PutMapping("/{id}")
  ```

  **変更後**:

  ```java
      @PutMapping("/{id}")
      @Operation(summary = "商品更新", description = "管理者が商品情報を更新")
  ```

---

- [ ] **T-5**: OrderController に @Tag, @Operation を追加

  パス: `backend/src/main/java/com/example/aiec/controller/OrderController.java`

  **変更前（1-3行目）**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.*;
  ```

  **変更後**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.*;
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.tags.Tag;
  ```

  **変更前（23-26行目）**:

  ```java
  @RestController
  @RequestMapping("/api/order")
  @RequiredArgsConstructor
  public class OrderController {
  ```

  **変更後**:

  ```java
  @RestController
  @RequestMapping("/api/order")
  @RequiredArgsConstructor
  @Tag(name = "注文", description = "注文の作成・取得・状態更新")
  public class OrderController {
  ```

  **各メソッドに @Operation 追加**（`@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` の直後に挿入）:

  | メソッド | 追加行 | アノテーション |
  |---------|--------|--------------|
  | getCart (38行目) | `@GetMapping("/cart")` の後 | `@Operation(summary = "カート取得", description = "セッションに紐づくカートを取得")` |
  | addToCart (50行目) | `@PostMapping("/cart/items")` の後 | `@Operation(summary = "カートに商品追加", description = "カートに商品を追加")` |
  | updateCartItem (63行目) | `@PutMapping("/cart/items/{id}")` の後 | `@Operation(summary = "カート内商品の数量変更", description = "カート内商品の数量を変更")` |
  | removeFromCart (77行目) | `@DeleteMapping("/cart/items/{id}")` の後 | `@Operation(summary = "カートから商品削除", description = "カートから商品を削除")` |
  | createOrder (89行目) | `@PostMapping` の後 | `@Operation(summary = "注文作成", description = "カートの内容で注文を作成")` |
  | getOrderById (115行目) | `@GetMapping("/{id}")` の後 | `@Operation(summary = "注文詳細取得", description = "指定IDの注文詳細を取得")` |
  | getOrderHistory (140行目) | `@GetMapping("/history")` の後 | `@Operation(summary = "注文履歴取得", description = "会員の注文履歴を取得")` |
  | cancelOrder (155行目) | `@PostMapping("/{id}/cancel")` の後 | `@Operation(summary = "注文キャンセル", description = "注文をキャンセルし在庫を戻す")` |
  | confirmOrder (181行目) | `@PostMapping("/{id}/confirm")` の後 | `@Operation(summary = "注文確認", description = "管理者が注文を確認済みにする")` |
  | shipOrder (205行目) | `@PostMapping("/{id}/ship")` の後 | `@Operation(summary = "注文発送", description = "管理者が注文を発送済みにする")` |
  | deliverOrder (229行目) | `@PostMapping("/{id}/deliver")` の後 | `@Operation(summary = "注文配達完了", description = "管理者が注文を配達完了にする")` |
  | getAllOrders (253行目) | `@GetMapping` の後 | `@Operation(summary = "全注文取得", description = "管理者が全注文を取得")` |

---

- [ ] **T-6**: AuthController に @Tag, @Operation を追加

  パス: `backend/src/main/java/com/example/aiec/controller/AuthController.java`

  **変更前（1-3行目）**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.*;
  ```

  **変更後**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.*;
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.tags.Tag;
  ```

  **変更前（18-21行目）**:

  ```java
  @RestController
  @RequestMapping("/api/auth")
  @RequiredArgsConstructor
  public class AuthController {
  ```

  **変更後**:

  ```java
  @RestController
  @RequestMapping("/api/auth")
  @RequiredArgsConstructor
  @Tag(name = "顧客認証", description = "顧客の登録・ログイン・ログアウト")
  public class AuthController {
  ```

  **各メソッドに @Operation 追加**:

  | メソッド | 挿入位置 | アノテーション |
  |---------|---------|--------------|
  | register | `@PostMapping("/register")` の後 | `@Operation(summary = "会員登録", description = "新規会員を登録しトークンを発行")` |
  | login | `@PostMapping("/login")` の後 | `@Operation(summary = "ログイン", description = "メールアドレスとパスワードで認証しトークンを発行")` |
  | logout | `@PostMapping("/logout")` の後 | `@Operation(summary = "ログアウト", description = "認証トークンを失効させる")` |
  | getCurrentUser | `@GetMapping("/me")` の後 | `@Operation(summary = "会員情報取得", description = "認証済み会員の情報を取得")` |

---

- [ ] **T-7**: BoAuthController に @Tag, @Operation を追加

  パス: `backend/src/main/java/com/example/aiec/controller/BoAuthController.java`

  **変更前（1-3行目）**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.*;
  ```

  **変更後**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.*;
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.tags.Tag;
  ```

  **変更前（17-20行目）**:

  ```java
  @RestController
  @RequestMapping("/api/bo-auth")
  @RequiredArgsConstructor
  public class BoAuthController {
  ```

  **変更後**:

  ```java
  @RestController
  @RequestMapping("/api/bo-auth")
  @RequiredArgsConstructor
  @Tag(name = "管理者認証", description = "管理者のログイン・ログアウト")
  public class BoAuthController {
  ```

  **各メソッドに @Operation 追加**:

  | メソッド | 挿入位置 | アノテーション |
  |---------|---------|--------------|
  | login | `@PostMapping("/login")` の後 | `@Operation(summary = "管理者ログイン", description = "BoUserのメールアドレスとパスワードで認証")` |
  | logout | `@PostMapping("/logout")` の後 | `@Operation(summary = "管理者ログアウト", description = "管理者の認証トークンを失効させる")` |
  | getCurrentBoUser | `@GetMapping("/me")` の後 | `@Operation(summary = "管理者情報取得", description = "認証済み管理者の情報を取得")` |

---

- [ ] **T-8**: BoAdminController に @Tag, @Operation を追加

  パス: `backend/src/main/java/com/example/aiec/controller/BoAdminController.java`

  **変更前（1-4行目）**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.ApiResponse;
  import com.example.aiec.dto.MemberDetailDto;
  ```

  **変更後**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.ApiResponse;
  import com.example.aiec.dto.MemberDetailDto;
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.tags.Tag;
  ```

  **変更前（24-27行目）**:

  ```java
  @RestController
  @RequestMapping("/api/bo/admin/members")
  @RequiredArgsConstructor
  public class BoAdminController {
  ```

  **変更後**:

  ```java
  @RestController
  @RequestMapping("/api/bo/admin/members")
  @RequiredArgsConstructor
  @Tag(name = "管理（会員）", description = "会員管理")
  public class BoAdminController {
  ```

  **各メソッドに @Operation 追加**:

  | メソッド | 挿入位置 | アノテーション |
  |---------|---------|--------------|
  | getMembers | `@GetMapping` の後 | `@Operation(summary = "会員一覧取得", description = "全会員の一覧を取得")` |
  | getMemberById | `@GetMapping("/{id}")` の後 | `@Operation(summary = "会員詳細取得", description = "指定IDの会員詳細と注文サマリを取得")` |
  | updateMemberStatus | `@PutMapping("/{id}/status")` の後 | `@Operation(summary = "会員状態変更", description = "会員の有効/無効状態を変更")` |

---

- [ ] **T-9**: BoAdminInventoryController に @Tag, @Operation を追加

  パス: `backend/src/main/java/com/example/aiec/controller/BoAdminInventoryController.java`

  **変更前（1-4行目）**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.ApiResponse;
  import com.example.aiec.dto.InventoryStatusDto;
  ```

  **変更後**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.ApiResponse;
  import com.example.aiec.dto.InventoryStatusDto;
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.tags.Tag;
  ```

  **変更前（19-22行目）**:

  ```java
  @RestController
  @RequestMapping("/api/bo/admin/inventory")
  @RequiredArgsConstructor
  public class BoAdminInventoryController {
  ```

  **変更後**:

  ```java
  @RestController
  @RequestMapping("/api/bo/admin/inventory")
  @RequiredArgsConstructor
  @Tag(name = "管理（在庫）", description = "在庫管理・調整")
  public class BoAdminInventoryController {
  ```

  **各メソッドに @Operation 追加**:

  | メソッド | 挿入位置 | アノテーション |
  |---------|---------|--------------|
  | getAllInventory | `@GetMapping` の後 | `@Operation(summary = "在庫一覧取得", description = "全商品の在庫状況を取得")` |
  | adjustStock | `@PostMapping("/adjust")` の後 | `@Operation(summary = "在庫調整", description = "指定商品の在庫数を調整")` |
  | getAdjustments | `@GetMapping("/adjustments")` の後 | `@Operation(summary = "在庫調整履歴取得", description = "在庫調整の履歴を取得")` |

---

- [ ] **T-10**: BoAdminBoUsersController に @Tag, @Operation を追加

  パス: `backend/src/main/java/com/example/aiec/controller/BoAdminBoUsersController.java`

  **変更前（1-4行目）**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.ApiResponse;
  import com.example.aiec.dto.BoUserDto;
  ```

  **変更後**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.ApiResponse;
  import com.example.aiec.dto.BoUserDto;
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.tags.Tag;
  ```

  **変更前（19-22行目）**:

  ```java
  @RestController
  @RequestMapping("/api/bo/admin/bo-users")
  @RequiredArgsConstructor
  public class BoAdminBoUsersController {
  ```

  **変更後**:

  ```java
  @RestController
  @RequestMapping("/api/bo/admin/bo-users")
  @RequiredArgsConstructor
  @Tag(name = "管理（BoUser）", description = "BoUser管理")
  public class BoAdminBoUsersController {
  ```

  **各メソッドに @Operation 追加**:

  | メソッド | 挿入位置 | アノテーション |
  |---------|---------|--------------|
  | getBoUsers | `@GetMapping` の後 | `@Operation(summary = "BoUser一覧取得", description = "全管理者ユーザーの一覧を取得")` |
  | createBoUser | `@PostMapping` の後 | `@Operation(summary = "BoUser作成", description = "新規管理者ユーザーを作成")` |

---

- [ ] **T-11**: InventoryController に @Tag, @Operation を追加

  パス: `backend/src/main/java/com/example/aiec/controller/InventoryController.java`

  **変更前（1-3行目）**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.*;
  ```

  **変更後**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.*;
  import io.swagger.v3.oas.annotations.Operation;
  import io.swagger.v3.oas.annotations.tags.Tag;
  ```

  **変更前（15-18行目）**:

  ```java
  @RestController
  @RequestMapping("/api/inventory")
  @RequiredArgsConstructor
  public class InventoryController {
  ```

  **変更後**:

  ```java
  @RestController
  @RequestMapping("/api/inventory")
  @RequiredArgsConstructor
  @Tag(name = "在庫（内部）", description = "在庫引当の内部API")
  public class InventoryController {
  ```

  **各メソッドに @Operation 追加**:

  | メソッド | 挿入位置 | アノテーション |
  |---------|---------|--------------|
  | createReservation | `@PostMapping("/reservations")` の後 | `@Operation(summary = "仮引当作成", description = "指定商品の在庫を仮引当する")` |
  | releaseReservation | `@DeleteMapping("/reservations")` の後 | `@Operation(summary = "仮引当解除", description = "仮引当を解除して在庫を戻す")` |
  | commitReservations | `@PostMapping("/reservations/commit")` の後 | `@Operation(summary = "本引当", description = "仮引当を本引当に確定する")` |
  | releaseCommittedReservations | `@PostMapping("/reservations/release")` の後 | `@Operation(summary = "本引当解除", description = "本引当を解除して在庫を戻す")` |
  | getAvailability | `@GetMapping("/availability/{productId}")` の後 | `@Operation(summary = "有効在庫確認", description = "指定商品の有効在庫数を取得")` |

---

- [ ] **T-12**: TestController に @Hidden を追加

  パス: `backend/src/main/java/com/example/aiec/controller/TestController.java`

  **変更前（1-3行目）**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.ApiResponse;
  ```

  **変更後**:

  ```java
  package com.example.aiec.controller;

  import com.example.aiec.dto.ApiResponse;
  import io.swagger.v3.oas.annotations.Hidden;
  ```

  **変更前（9-12行目）**:

  ```java
  @RestController
  @Profile("!production-internal")
  @RequestMapping("/test")
  public class TestController {
  ```

  **変更後**:

  ```java
  @RestController
  @Profile("!production-internal")
  @RequestMapping("/test")
  @Hidden
  public class TestController {
  ```

---

- [ ] **T-13**: 主要 DTO に @Schema を追加

  以下の各ファイルにアノテーションを追加する。import は各ファイル先頭に `import io.swagger.v3.oas.annotations.media.Schema;` を追加。

  #### ProductDto.java

  パス: `backend/src/main/java/com/example/aiec/dto/ProductDto.java`

  **変更前（13-24行目）**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class ProductDto {

      private Long id;
      private String name;
      private BigDecimal price;
      private String image;
      private String description;
      private Integer stock;
      private Boolean isPublished;
  ```

  **変更後**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "商品情報")
  public class ProductDto {

      @Schema(description = "商品ID", example = "1")
      private Long id;
      @Schema(description = "商品名", example = "AIスピーカー")
      private String name;
      @Schema(description = "税込価格（円）", example = "3980")
      private BigDecimal price;
      @Schema(description = "商品画像URL", example = "/images/speaker.jpg")
      private String image;
      @Schema(description = "商品説明")
      private String description;
      @Schema(description = "在庫数", example = "100")
      private Integer stock;
      @Schema(description = "公開状態", example = "true")
      private Boolean isPublished;
  ```

  #### ProductListResponse.java

  パス: `backend/src/main/java/com/example/aiec/dto/ProductListResponse.java`

  **変更前（12-21行目）**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class ProductListResponse {

      private List<ProductDto> items;
      private Long total;
      private Integer page;
      private Integer limit;
  ```

  **変更後**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "商品一覧レスポンス")
  public class ProductListResponse {

      @Schema(description = "商品リスト")
      private List<ProductDto> items;
      @Schema(description = "総件数", example = "50")
      private Long total;
      @Schema(description = "現在のページ番号", example = "1")
      private Integer page;
      @Schema(description = "1ページあたりの件数", example = "20")
      private Integer limit;
  ```

  #### ApiResponse.java

  パス: `backend/src/main/java/com/example/aiec/dto/ApiResponse.java`

  **変更前（12-15行目）**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public class ApiResponse<T> {

      private boolean success;
      private T data;
      private ErrorDetail error;
  ```

  **変更後**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "API共通レスポンス")
  public class ApiResponse<T> {

      @Schema(description = "成功フラグ", example = "true")
      private boolean success;
      @Schema(description = "レスポンスデータ")
      private T data;
      @Schema(description = "エラー詳細（エラー時のみ）")
      private ErrorDetail error;
  ```

  **ErrorDetail 内部クラス（45-48行目）**:

  変更前:

  ```java
      public static class ErrorDetail {
          private String code;
          private String message;
          @JsonInclude(JsonInclude.Include.NON_NULL)
          private Object details;
  ```

  変更後:

  ```java
      @Schema(description = "エラー詳細")
      public static class ErrorDetail {
          @Schema(description = "エラーコード", example = "NOT_FOUND")
          private String code;
          @Schema(description = "エラーメッセージ", example = "リソースが見つかりません")
          private String message;
          @JsonInclude(JsonInclude.Include.NON_NULL)
          @Schema(description = "追加のエラー情報")
          private Object details;
  ```

  #### CartDto.java

  パス: `backend/src/main/java/com/example/aiec/dto/CartDto.java`

  **変更前（15-18行目）**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class CartDto {

      private List<CartItemDto> items;
      private Integer totalQuantity;
      private BigDecimal totalPrice;
  ```

  **変更後**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "カート情報")
  public class CartDto {

      @Schema(description = "カート内商品リスト")
      private List<CartItemDto> items;
      @Schema(description = "合計数量", example = "3")
      private Integer totalQuantity;
      @Schema(description = "合計金額（円）", example = "11940")
      private BigDecimal totalPrice;
  ```

  #### CartItemDto.java

  パス: `backend/src/main/java/com/example/aiec/dto/CartItemDto.java`

  **変更前（11-14行目）**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class CartItemDto {

      private Long id;
      private ProductDto product;
      private Integer quantity;
  ```

  **変更後**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "カートアイテム情報")
  public class CartItemDto {

      @Schema(description = "商品ID", example = "1")
      private Long id;
      @Schema(description = "商品情報")
      private ProductDto product;
      @Schema(description = "数量", example = "2")
      private Integer quantity;
  ```

  #### OrderDto.java

  パス: `backend/src/main/java/com/example/aiec/dto/OrderDto.java`

  **変更前（17-20行目）**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class OrderDto {

      private Long orderId;
      private String orderNumber;
      private Long userId;
      private String userEmail;
      private String userDisplayName;
      private List<OrderItemDto> items;
      private BigDecimal totalPrice;
      private String status;
      private String createdAt;
      private String updatedAt;
  ```

  **変更後**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "注文情報")
  public class OrderDto {

      @Schema(description = "注文ID", example = "1")
      private Long orderId;
      @Schema(description = "注文番号", example = "ORD-20260218-001")
      private String orderNumber;
      @Schema(description = "会員ID（ゲスト注文の場合null）", example = "1")
      private Long userId;
      @Schema(description = "会員メールアドレス")
      private String userEmail;
      @Schema(description = "会員表示名")
      private String userDisplayName;
      @Schema(description = "注文商品リスト")
      private List<OrderItemDto> items;
      @Schema(description = "合計金額（円）", example = "11940")
      private BigDecimal totalPrice;
      @Schema(description = "注文ステータス", example = "PENDING")
      private String status;
      @Schema(description = "注文日時（ISO 8601）")
      private String createdAt;
      @Schema(description = "更新日時（ISO 8601）")
      private String updatedAt;
  ```

  #### OrderItemDto.java

  パス: `backend/src/main/java/com/example/aiec/dto/OrderItemDto.java`

  **変更前（13-16行目）**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class OrderItemDto {

      private ProductDto product;
      private Integer quantity;
      private BigDecimal subtotal;
  ```

  **変更後**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "注文アイテム情報")
  public class OrderItemDto {

      @Schema(description = "商品情報")
      private ProductDto product;
      @Schema(description = "数量", example = "2")
      private Integer quantity;
      @Schema(description = "小計（円）", example = "7960")
      private BigDecimal subtotal;
  ```

  #### UserDto.java

  パス: `backend/src/main/java/com/example/aiec/dto/UserDto.java`

  **変更前（13-16行目）**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public class UserDto {

      private Long id;
      private String email;
      private String displayName;
      private Boolean isActive;
      private Instant createdAt;
  ```

  **変更後**:

  ```java
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "ユーザー情報")
  public class UserDto {

      @Schema(description = "ユーザーID", example = "1")
      private Long id;
      @Schema(description = "メールアドレス", example = "user@example.com")
      private String email;
      @Schema(description = "表示名", example = "田中太郎")
      private String displayName;
      @Schema(description = "有効状態", example = "true")
      private Boolean isActive;
      @Schema(description = "登録日時")
      private Instant createdAt;
  ```

  #### BoUserDto.java

  パス: `backend/src/main/java/com/example/aiec/dto/BoUserDto.java`

  **変更前（8-9行目）**:

  ```java
  @Data
  public class BoUserDto {
      private Long id;
      private String email;
      private String displayName;
      private PermissionLevel permissionLevel;
      private Instant lastLoginAt;
      private Boolean isActive;
      private Instant createdAt;
      private Instant updatedAt;
  ```

  **変更後**:

  ```java
  @Data
  @Schema(description = "管理者ユーザー情報")
  public class BoUserDto {
      @Schema(description = "管理者ID", example = "1")
      private Long id;
      @Schema(description = "メールアドレス", example = "admin@example.com")
      private String email;
      @Schema(description = "表示名", example = "管理者")
      private String displayName;
      @Schema(description = "権限レベル", example = "ADMIN")
      private PermissionLevel permissionLevel;
      @Schema(description = "最終ログイン日時")
      private Instant lastLoginAt;
      @Schema(description = "有効状態", example = "true")
      private Boolean isActive;
      @Schema(description = "作成日時")
      private Instant createdAt;
      @Schema(description = "更新日時")
      private Instant updatedAt;
  ```

---

### ドキュメント

- [ ] **T-14**: 承認済み OpenAPI スナップショットの初回作成

  パス: `docs/api-contract/openapi.json`（新規作成）

  以下のコマンドで生成:

  ```bash
  docker compose exec backend ./mvnw verify -DskipTests
  docker compose exec backend cat target/openapi.json > docs/api-contract/openapi.json
  ```

  ※ T-1〜T-13 の実装完了後に実行すること。

---

## 実装順序

```
T-1（pom.xml 依存追加）
  → T-2（application.yml 設定）, T-3（OpenApiConfig 新規作成）（並行可能）
    → T-4〜T-12（コントローラ・DTO アノテーション追加 — すべて並行可能）
      → T-13（DTO @Schema 追加 — T-4〜T-12 と並行可能）
        → T-14（スナップショット生成 — 全タスク完了後）
```

T-1 が最初に必要（依存追加しないとアノテーションが解決できない）。
T-2, T-3 は T-1 完了後に並行可能。
T-4〜T-13 はすべて独立した変更のため並行可能。
T-14 は全タスク完了後にのみ実行可能。

---

## テスト手順

実装後に以下を確認:

1. `docker compose exec backend ./mvnw compile` → コンパイル成功
2. `docker compose exec backend ./mvnw test` → 既存テスト全パス（ロジック変更なし）
3. `curl http://localhost:8080/v3/api-docs` → OpenAPI 3.0 JSON が返却される
4. ブラウザで `http://localhost:8080/swagger-ui.html` → Swagger UI が表示される
5. Swagger UI で全エンドポイントが @Tag ごとにグループ化されている
6. `/test` 系エンドポイントが Swagger UI に表示されていない（@Hidden）
7. Schemas セクションで DTO の description, example が表示されている
8. `docker compose exec backend ./mvnw verify -DskipTests` → `target/openapi.json` が生成される

## Review Packet
### 変更サマリ（10行以内）
- backend/pom.xml に springdoc 依存と Maven プラグインを追加
- application.yml に springdoc 設定を追加
- OpenAPI セキュリティ定義用の OpenApiConfig を新規作成
- 各コントローラに @Tag/@Operation/@Parameter/@Hidden を付与し公開範囲を明示
- 主要 DTO に @Schema を付与し description と example を定義
### 変更ファイル一覧
- backend/pom.xml
- backend/src/main/resources/application.yml
- backend/src/main/java/com/example/aiec/config/OpenApiConfig.java
- backend/src/main/java/com/example/aiec/controller/ItemController.java
- backend/src/main/java/com/example/aiec/controller/OrderController.java
- backend/src/main/java/com/example/aiec/controller/AuthController.java
- backend/src/main/java/com/example/aiec/controller/BoAuthController.java
- backend/src/main/java/com/example/aiec/controller/BoAdminController.java
- backend/src/main/java/com/example/aiec/controller/BoAdminInventoryController.java
- backend/src/main/java/com/example/aiec/controller/BoAdminBoUsersController.java
- backend/src/main/java/com/example/aiec/controller/InventoryController.java
- backend/src/main/java/com/example/aiec/controller/TestController.java
- backend/src/main/java/com/example/aiec/dto/ProductDto.java
- backend/src/main/java/com/example/aiec/dto/ProductListResponse.java
- backend/src/main/java/com/example/aiec/dto/ApiResponse.java
- backend/src/main/java/com/example/aiec/dto/CartDto.java
- backend/src/main/java/com/example/aiec/dto/CartItemDto.java
- backend/src/main/java/com/example/aiec/dto/OrderDto.java
- backend/src/main/java/com/example/aiec/dto/OrderItemDto.java
- backend/src/main/java/com/example/aiec/dto/UserDto.java
- backend/src/main/java/com/example/aiec/dto/BoUserDto.java
### リスクと未解決
- T-14: Maven Wrapper がネットワーク制限で Maven 本体を取得できず実行不可のため openapi.json 未生成
- 依存キャッシュがない環境では同様にテスト/ビルド実行がブロックされる可能性
### テスト結果
- `docker compose exec backend ./mvnw compile` : [FAIL] - backend コンテナに mvnw 不在 (app.jar のみ)
- `docker compose exec backend ./mvnw test` : [FAIL] - 上記と同理由
- `curl http://localhost:8080/v3/api-docs` : [FAIL] - 未実行（API サーバ起動状態未確認）
- `http://localhost:8080/swagger-ui.html` : [FAIL] - 未実行（API サーバ起動状態未確認）
- Swagger UI のタグ分割確認 : [FAIL] - 未実行（UI 未起動）
- `/test` 系エンドポイント非表示確認 : [FAIL] - 未実行
- DTO Schema 表示確認 : [FAIL] - 未実行
- `docker compose exec backend ./mvnw verify -DskipTests` : [FAIL] - backend コンテナに mvnw 不在; ホストでの mvnw 実行も Maven ダウンロードがネットワーク制限で失敗
