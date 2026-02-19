# CHG-013: モジュラーモノリス設計 Phase 3 - 実装タスク

要件: `docs/01_requirements/CHG-013_モジュラーモノリス設計.md`
設計: `docs/02_designs/CHG-013_モジュラーモノリス設計.md`
作成日: 2026-02-18

検証コマンド:
- バックエンド: `docker compose exec backend ./mvnw compile`
- 全テスト: `docker compose exec backend ./mvnw test`
- ArchUnitテストのみ: `docker compose exec backend ./mvnw test -Dtest=ModularMonolithArchitectureTest`
- コンテナ未起動の場合: `docker compose up -d` を先に実行

---

## Phase 3 概要

Phase 3では、ArchUnitを導入してモジュール境界違反を自動検出するテストを追加します。

**対象**: backend のアーキテクチャテスト

**移行方針**:
1. ArchUnit依存関係を追加（pom.xml）
2. アーキテクチャテストクラスを作成
3. モジュール境界違反の検出ルールを定義
4. テスト実行して違反0件を確認

**前提条件**: Phase 2（Port抽出）が完了していること

---

## タスク一覧

### 依存関係追加

- [ ] **T-1**: ArchUnit依存関係の追加

  パス: `backend/pom.xml`

  **変更箇所**: `<dependencies>` セクション内に追加

  挿入位置: `<dependencies>` タグの末尾（他のtest依存関係の近く）

  ```xml
  <!-- ArchUnit: アーキテクチャテスト -->
  <dependency>
      <groupId>com.tngtech.archunit</groupId>
      <artifactId>archunit-junit5</artifactId>
      <version>1.2.1</version>
      <scope>test</scope>
  </dependency>
  ```

---

### テストディレクトリ作成

- [ ] **T-2**: アーキテクチャテスト用ディレクトリの作成

  ```bash
  mkdir -p backend/src/test/java/com/example/aiec/architecture
  ```

---

### アーキテクチャテストクラス作成

- [ ] **T-3**: ModularMonolithArchitectureTest クラス作成

  パス: `backend/src/test/java/com/example/aiec/architecture/ModularMonolithArchitectureTest.java`

  新規ファイル作成:

  ```java
  package com.example.aiec.architecture;

  import com.tngtech.archunit.core.domain.JavaClasses;
  import com.tngtech.archunit.core.importer.ClassFileImporter;
  import com.tngtech.archunit.core.importer.ImportOption;
  import com.tngtech.archunit.lang.ArchRule;
  import org.junit.jupiter.api.BeforeAll;
  import org.junit.jupiter.api.Test;

  import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
  import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

  /**
   * モジュラーモノリスのアーキテクチャルール検証テスト
   */
  public class ModularMonolithArchitectureTest {

      private static JavaClasses classes;

      @BeforeAll
      public static void setUp() {
          classes = new ClassFileImporter()
                  .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                  .importPackages("com.example.aiec");
      }

      /**
       * ルール1: domain層は他モジュールのdomain層を直接参照してはいけない
       */
      @Test
      void domainLayerShouldNotAccessOtherModuleDomain() {
          ArchRule rule = noClasses()
                  .that().resideInAPackage("..modules.*.domain..")
                  .should().dependOnClassesThat().resideInAPackage("..modules.*.domain..")
                  .because("domain層は他モジュールのdomain層を直接参照してはいけません（Port経由で参照してください）");

          // 自モジュール内の参照は許可するため、違反を手動フィルタリング
          // 簡易版として、全体チェックのみ実施
          rule.check(classes);
      }

      /**
       * ルール2: 他モジュールはapplication.portのみ参照可能
       */
      @Test
      void otherModulesShouldOnlyAccessThroughPort() {
          ArchRule rule = noClasses()
                  .that().resideInAPackage("..modules.product..")
                  .should().dependOnClassesThat().resideInAnyPackage(
                          "..modules.inventory.domain..",
                          "..modules.inventory.adapter..",
                          "..modules.purchase.cart..",
                          "..modules.purchase.order..",
                          "..modules.customer.domain..",
                          "..modules.customer.adapter..",
                          "..modules.backoffice.domain..",
                          "..modules.backoffice.adapter.."
                  )
                  .because("他モジュールはapplication.portのみ参照可能です");

          rule.check(classes);
      }

      /**
       * ルール3: adapter層はdomain層に直接依存してはいけない
       */
      @Test
      void adapterShouldNotAccessDomainDirectly() {
          ArchRule rule = noClasses()
                  .that().resideInAPackage("..modules.*.adapter..")
                  .should().dependOnClassesThat().resideInAPackage("..modules.*.domain..")
                  .because("adapter層はdomain層に直接依存してはいけません（Port経由で参照してください）");

          // 自モジュール内のentityへの参照は許可（DTO変換で使用）
          // 簡易版として、全体チェックのみ実施
          rule.check(classes);
      }

      /**
       * ルール4: shared以外のモジュールはsharedのみ依存可能
       */
      @Test
      void modulesShouldOnlyDependOnShared() {
          ArchRule rule = classes()
                  .that().resideInAPackage("..modules..")
                  .and().resideOutsideOfPackage("..modules.shared..")
                  .should().onlyDependOnClassesThat()
                  .resideInAnyPackage(
                          "..modules.shared..",
                          "java..",
                          "javax..",
                          "jakarta..",
                          "org.springframework..",
                          "org.hibernate..",
                          "lombok..",
                          "com.fasterxml.jackson..",
                          "org.slf4j.."
                  )
                  .orShould().resideInAPackage("..modules..")
                  .because("モジュールはsharedモジュールのみ依存可能です");

          // 注: このルールは厳格すぎる可能性があるため、違反がある場合は調整が必要
          rule.check(classes);
      }

      /**
       * ルール5: Port実装クラスはパッケージプライベートであること
       */
      @Test
      void portImplementationsShouldBePackagePrivate() {
          ArchRule rule = classes()
                  .that().resideInAPackage("..modules.*.application.usecase..")
                  .and().haveSimpleNameEndingWith("UseCase")
                  .should().bePackagePrivate()
                  .because("Port実装クラス（UseCase）はパッケージプライベートにして、Portインターフェース経由でのみアクセスさせてください");

          rule.check(classes);
      }

      /**
       * ルール6: Portインターフェースはpublicであること
       */
      @Test
      void portInterfacesShouldBePublic() {
          ArchRule rule = classes()
                  .that().resideInAPackage("..modules.*.application.port..")
                  .should().bePublic()
                  .because("Portインターフェースは他モジュールから参照されるため、publicでなければなりません");

          rule.check(classes);
      }

      /**
       * ルール7: エンティティはdomain.entityパッケージに配置されていること
       */
      @Test
      void entitiesShouldResideInDomainEntityPackage() {
          ArchRule rule = classes()
                  .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                  .should().resideInAPackage("..modules.*.domain.entity..")
                  .orShould().resideInAPackage("..modules.*.cart.entity..")  // purchase.cart 例外
                  .orShould().resideInAPackage("..modules.*.order.entity..") // purchase.order 例外
                  .because("エンティティはdomain.entityパッケージに配置してください");

          rule.check(classes);
      }

      /**
       * ルール8: リポジトリはdomain.repositoryパッケージに配置されていること
       */
      @Test
      void repositoriesShouldResideInDomainRepositoryPackage() {
          ArchRule rule = classes()
                  .that().haveSimpleNameEndingWith("Repository")
                  .and().areInterfaces()
                  .should().resideInAPackage("..modules.*.domain.repository..")
                  .orShould().resideInAPackage("..modules.*.cart.repository..")  // purchase.cart 例外
                  .orShould().resideInAPackage("..modules.*.order.repository..") // purchase.order 例外
                  .because("リポジトリはdomain.repositoryパッケージに配置してください");

          rule.check(classes);
      }

      /**
       * ルール9: Controllerはadapter.restパッケージに配置されていること
       */
      @Test
      void controllersShouldResideInAdapterRestPackage() {
          ArchRule rule = classes()
                  .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                  .should().resideInAPackage("..modules.*.adapter.rest..")
                  .orShould().resideInAPackage("..config..")  // TestController 例外
                  .because("Controllerはadapter.restパッケージに配置してください");

          rule.check(classes);
      }

      /**
       * ルール10: DTOはadapter.dtoまたはshared.dtoパッケージに配置されていること
       */
      @Test
      void dtosShouldResideInAdapterDtoPackage() {
          ArchRule rule = classes()
                  .that().haveSimpleNameEndingWith("Dto")
                  .or().haveSimpleNameEndingWith("Request")
                  .or().haveSimpleNameEndingWith("Response")
                  .should().resideInAPackage("..modules.*.adapter.dto..")
                  .orShould().resideInAPackage("..modules.shared.dto..")
                  .because("DTOはadapter.dtoまたはshared.dtoパッケージに配置してください");

          rule.check(classes);
      }

  }
  ```

  **ポイント**:
  - Phase 1, 2で構築したモジュール構造に対して10個のルールを定義
  - ルール1〜4: モジュール間の依存関係制約
  - ルール5〜6: Port実装の可視性制約
  - ルール7〜10: パッケージ配置規約

---

### 統合作業

- [ ] **T-4**: コンパイル確認

  ```bash
  docker compose exec backend ./mvnw compile
  ```

  コンパイルエラーが出た場合は import 文を確認。

---

- [ ] **T-5**: ArchUnitテスト実行

  ```bash
  docker compose exec backend ./mvnw test -Dtest=ModularMonolithArchitectureTest
  ```

  **期待結果**: 全テストがパス（違反0件）

  **違反が出た場合の対処**:
  1. エラーメッセージから違反箇所を特定
  2. 以下のパターンで修正:
     - 他モジュールのdomain層を直接参照している → Port経由に修正
     - adapter層がdomain層を直接参照している → Port経由に修正
     - エンティティ・リポジトリ・Controller・DTOのパッケージ位置が間違っている → 正しい位置に移動
     - UseCase クラスがpublicになっている → パッケージプライベート（`class`）に変更
  3. 再度テスト実行

---

- [ ] **T-6**: 全テスト実行確認

  ```bash
  docker compose exec backend ./mvnw test
  ```

  全テストがパスすることを確認。

---

## 実装順序

```
T-1（pom.xml依存関係追加）
  → T-2（テストディレクトリ作成）
    → T-3（アーキテクチャテストクラス作成）
      → T-4（コンパイル確認）
        → T-5（ArchUnitテスト実行）
          → T-6（全テスト実行確認）
```

---

## テスト手順

Phase 3完了後に以下を確認:

1. `docker compose exec backend ./mvnw compile` → コンパイル成功
2. `docker compose exec backend ./mvnw test -Dtest=ModularMonolithArchitectureTest` → 全ルールがパス（違反0件）
3. `docker compose exec backend ./mvnw test` → 既存テスト含め全パス
4. `backend/src/test/java/com/example/aiec/architecture/ModularMonolithArchitectureTest.java` が存在
5. pom.xml に ArchUnit 依存関係が追加されている

---

## よくある違反パターンと修正方法

| 違反パターン | 修正方法 |
|------------|---------|
| OrderService が InventoryService を直接参照 | InventoryCommandPort を参照するように変更 |
| ProductController が ProductUseCase を直接参照 | ProductQueryPort / ProductCommandPort を参照 |
| ProductUseCase が public クラス | `class ProductUseCase` に変更（パッケージプライベート） |
| ProductDto が modules.product.domain.dto パッケージにある | modules.product.adapter.dto に移動 |
| ProductRepository が modules.product.repository にある | modules.product.domain.repository に移動 |

---

## Phase 4 への準備

Phase 3完了後、以下を実施予定:
- **Phase 4**: 監査ログイベント実装（OperationPerformedEvent、REQUIRES_NEW）

Phase 4の詳細は別途タスクファイルを参照。
