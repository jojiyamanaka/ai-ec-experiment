# CHG-012: API契約管理 - 技術設計

要件: `docs/01_requirements/CHG-012_API契約管理.md`
作成日: 2026-02-18

---

## 1. 設計方針

- **Code-First**: springdoc-openapi でコードから OpenAPI 仕様を自動生成する
- **スコープ**: Core API のみ（BFF は薄いプロキシのため対象外）
- **段階的導入**: まずライブラリ導入とアノテーション追加、次に破壊的変更検知の自動化
- **既存コードへの影響最小化**: アノテーション追加のみ、ロジック変更なし

### 全体構成

```
[コード + アノテーション]
    ↓ springdoc-openapi（自動生成）
[OpenAPI 3.0 仕様 (JSON)]  ←── /v3/api-docs で公開
    ↓
[Swagger UI]               ←── /swagger-ui.html で閲覧
    ↓ openapi-diff（CI）
[破壊的変更検知]            ←── スナップショットとの差分比較
```

---

## 2. 依存ライブラリ

### backend/pom.xml に追加

```xml
<!-- OpenAPI / Swagger UI -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.4</version>
</dependency>
```

この1つの依存で以下が有効になる:
- `/v3/api-docs` — OpenAPI 3.0 JSON 自動生成
- `/swagger-ui.html` — Swagger UI（ブラウザで API を確認・テスト）

---

## 3. バックエンド実装

### 3.1 OpenAPI 設定クラス（新規）

**ファイル**: `backend/src/main/java/com/example/aiec/config/OpenApiConfig.java`

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

### 3.2 application.yml に設定追加

**ファイル**: `backend/src/main/resources/application.yml`（または application.properties）

```yaml
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

### 3.3 コントローラへのアノテーション追加

既存の Javadoc コメントを活かしつつ、OpenAPI アノテーションを追加する。

**変更パターン（ItemController の例）**:

```java
// 変更前
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
public class ItemController {

    @GetMapping
    public ApiResponse<ProductListResponse> getProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {

// 変更後
@RestController
@RequestMapping("/api/item")
@RequiredArgsConstructor
@Tag(name = "商品", description = "商品の取得・更新")
public class ItemController {

    @GetMapping
    @Operation(summary = "商品一覧取得", description = "公開商品の一覧をページネーション付きで取得")
    public ApiResponse<ProductListResponse> getProducts(
            @RequestParam(defaultValue = "1") @Parameter(description = "ページ番号") int page,
            @RequestParam(defaultValue = "20") @Parameter(description = "1ページあたりの件数") int limit) {
```

### 3.4 コントローラ別の @Tag 定義

| コントローラ | @Tag name | description |
|---|---|---|
| ItemController | 商品 | 商品の取得・更新 |
| OrderController | 注文 | 注文の作成・取得・状態更新 |
| AuthController | 顧客認証 | 顧客の登録・ログイン・ログアウト |
| BoAuthController | 管理者認証 | 管理者のログイン・ログアウト |
| BoAdminController | 管理（会員） | 会員管理 |
| BoAdminInventoryController | 管理（在庫） | 在庫管理・調整 |
| BoAdminBoUsersController | 管理（BoUser） | BoUser管理 |
| InventoryController | 在庫（内部） | 在庫引当の内部API |

### 3.5 DTO への @Schema アノテーション追加

**変更パターン（ProductDto の例）**:

```java
// 変更前
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String name;
    private BigDecimal price;

// 変更後
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
```

### 3.6 ApiResponse のジェネリクス対応

springdoc は `ApiResponse<T>` のジェネリクスを自動解決するが、既存の `ApiResponse` クラス名が swagger-core の `io.swagger.v3.oas.models.responses.ApiResponse` と衝突する可能性がある。

**対応**: import 文で明示的に区別する。クラス名の変更は不要（springdoc は Java の型解析を使うため問題なし）。

### 3.7 TestController の除外

テスト用コントローラは API 仕様から除外する。

```java
@RestController
@RequestMapping("/api/test")
@Hidden  // OpenAPI 仕様から除外
public class TestController {
```

---

## 4. 破壊的変更の検知

### 4.1 設計思想: JSON ファイル同士の比較

稼働中サーバーへの `curl` ではなく、**ビルド時に静的に生成した JSON ファイル同士**を比較する。

```
承認済みスナップショット（Git管理）   現在のコードから生成したJSON
docs/api-contract/openapi.json    ←→  target/openapi.json
                                   ↓
                              openapi-diff
                                   ↓
                           差分レポート出力
```

**メリット**:
- CI でサーバー起動が不要（ビルドのみで完結）
- 再現性が高い（同じコードからは常に同じ JSON が生成される）
- オフラインでも差分チェック可能

### 4.2 ビルド時の OpenAPI JSON 自動生成

springdoc-openapi-maven-plugin を使い、`mvn verify` 時に JSON ファイルを自動生成する。

**backend/pom.xml に追加**:

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

※ このプラグインは `integration-test` フェーズで一時的にアプリを起動し、`/v3/api-docs` から JSON を取得して `target/openapi.json` に保存する。CI 上でもビルドコマンドだけで完結する。

生成コマンド:
```bash
cd backend
./mvnw verify -DskipTests
# → target/openapi.json が生成される
```

### 4.3 承認済みスナップショットの管理

**ファイル**: `docs/api-contract/openapi.json`（Git 管理）

このファイルはチームが承認した「現在の API 契約」を表す。API 変更時にのみ意図的に更新する。

スナップショットの初回作成・更新手順:
```bash
cd backend
./mvnw verify -DskipTests
cp target/openapi.json ../docs/api-contract/openapi.json
```

### 4.4 差分比較

[openapi-diff](https://github.com/OpenAPITools/openapi-diff) で **2つの JSON ファイル**を比較する。

```bash
# ローカル実行（Docker）
docker run --rm -v $(pwd):/work openapitools/openapi-diff:latest \
  /work/docs/api-contract/openapi.json \
  /work/backend/target/openapi.json \
  --markdown /work/docs/api-contract/diff-report.md

# 破壊的変更がある場合は exit code 1 を返す（CI 連携用）
docker run --rm -v $(pwd):/work openapitools/openapi-diff:latest \
  /work/docs/api-contract/openapi.json \
  /work/backend/target/openapi.json \
  --fail-on-incompatible
```

### 4.5 運用フロー

```
1. 開発者が API を変更
2. ./mvnw verify -DskipTests → target/openapi.json 生成
3. openapi-diff で承認済み JSON と比較
4. 差分の確認:
   ├─ 破壊的変更あり → 後方互換に修正、またはレビューで承認
   └─ 互換性のある変更のみ → OK
5. 承認後: cp target/openapi.json docs/api-contract/openapi.json
6. スナップショット更新をコミットに含める
```

### 4.6 将来の CI 統合イメージ

```yaml
# .github/workflows/api-contract.yml（参考・本CHGのスコープ外）
jobs:
  api-contract-check:
    steps:
      - uses: actions/checkout@v4
      - run: cd backend && ./mvnw verify -DskipTests
      - run: |
          docker run --rm -v ${{ github.workspace }}:/work \
            openapitools/openapi-diff:latest \
            /work/docs/api-contract/openapi.json \
            /work/backend/target/openapi.json \
            --fail-on-incompatible
```

---

## 5. 処理フロー

### springdoc による仕様生成フロー

```
アプリケーション起動
    ↓
springdoc がコントローラ・DTO をスキャン
    ↓
@Tag, @Operation, @Schema アノテーションを収集
    ↓
OpenAPI 3.0 仕様を自動生成
    ↓
/v3/api-docs で JSON 公開
/swagger-ui.html で UI 公開
```

### 破壊的変更チェックフロー（JSON同士の比較）

```
API 変更のコミット
    ↓
./mvnw verify -DskipTests
    ↓
target/openapi.json 生成（コードから静的に）
    ↓
openapi-diff:
  docs/api-contract/openapi.json（承認済み）
  vs target/openapi.json（現在のコード）
    ↓
Breaking Changes あり?
    ├─ Yes → 後方互換に修正 or レビュー承認
    └─ No → OK
    ↓
承認後: スナップショット更新 & コミット
```

---

## 6. 影響範囲

### 変更対象ファイル

| ファイル | 変更内容 |
|---------|---------|
| `backend/pom.xml` | springdoc-openapi 依存追加 + maven-plugin 追加 |
| `backend/src/.../config/OpenApiConfig.java` | **新規** OpenAPI 設定 |
| `backend/src/.../controller/ItemController.java` | @Tag, @Operation 追加 |
| `backend/src/.../controller/OrderController.java` | @Tag, @Operation 追加 |
| `backend/src/.../controller/AuthController.java` | @Tag, @Operation 追加 |
| `backend/src/.../controller/BoAuthController.java` | @Tag, @Operation 追加 |
| `backend/src/.../controller/BoAdminController.java` | @Tag, @Operation 追加 |
| `backend/src/.../controller/BoAdminInventoryController.java` | @Tag, @Operation 追加 |
| `backend/src/.../controller/BoAdminBoUsersController.java` | @Tag, @Operation 追加 |
| `backend/src/.../controller/TestController.java` | @Hidden 追加 |
| `backend/src/.../dto/ProductDto.java` | @Schema 追加 |
| `backend/src/.../dto/ProductListResponse.java` | @Schema 追加 |
| `backend/src/.../dto/CartDto.java` | @Schema 追加 |
| `backend/src/.../dto/CartItemDto.java` | @Schema 追加 |
| `backend/src/.../dto/OrderDto.java` | @Schema 追加 |
| `backend/src/.../dto/OrderItemDto.java` | @Schema 追加 |
| `backend/src/.../dto/UserDto.java` | @Schema 追加 |
| `backend/src/.../dto/BoUserDto.java` | @Schema 追加 |
| `backend/src/.../dto/ApiResponse.java` | @Schema 追加 |
| 他の Request/Response DTO | @Schema 追加 |
| `backend/src/main/resources/application.yml` | springdoc 設定追加 |
| `docs/api-contract/openapi.json` | **新規** 承認済みスナップショット（Git管理） |

### 影響なし

| ファイル | 理由 |
|---------|------|
| BFF (bff/) | スコープ外 |
| Frontend (frontend/) | 変更なし |
| Entity クラス | DTO のみにアノテーション |
| Service / Repository | ロジック変更なし |
| DB スキーマ | 変更なし |

---

## 7. テスト観点

| # | テスト内容 | 確認方法 |
|---|----------|---------|
| 1 | /v3/api-docs が JSON を返すこと | `curl http://localhost:8080/v3/api-docs` |
| 2 | /swagger-ui.html が表示されること | ブラウザでアクセス |
| 3 | 全エンドポイントが仕様に含まれること | Swagger UI で目視確認 |
| 4 | TestController が除外されていること | 仕様に /api/test が含まれないこと |
| 5 | DTO のフィールド説明が表示されること | Swagger UI の Schemas セクション確認 |
| 6 | 既存の API 動作に影響がないこと | `./mvnw test` が全て通ること |
| 7 | `./mvnw verify -DskipTests` で `target/openapi.json` が生成されること | ファイル存在確認 |
| 8 | openapi-diff で承認済み JSON との差分がないこと | `--fail-on-incompatible` が exit 0 |

---

## 8. 備考

### 将来の拡張（本CHGのスコープ外）

- **CI での自動チェック**: GitHub Actions で `./mvnw verify` + `openapi-diff --fail-on-incompatible` を実行（セクション 4.6 参照）
- **TypeScript 型生成**: openapi-generator で OpenAPI 仕様から `frontend/src/types/api.ts` を自動生成
- **BFF 契約管理**: @nestjs/swagger 導入による UI-BFF 間の契約管理
- **Contract Testing**: Spring Cloud Contract や Pact による BFF-Core API 間のコンシューマ駆動テスト
