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
 *
 * 各モジュールは以下のパッケージ構成を持つ:
 *   modules/{module}/domain/entity      - エンティティ（JPA）
 *   modules/{module}/domain/repository  - リポジトリインターフェース
 *   modules/{module}/domain/service     - ドメインサービス
 *   modules/{module}/application/port   - 公開インターフェース（Port）
 *   modules/{module}/application/usecase - Port実装（package-private）
 *   modules/{module}/adapter/rest       - Controller
 *   modules/{module}/adapter/dto        - DTO
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
     * ルール1: product.domain は最下位レイヤーであり他モジュールのdomain層に依存しない
     *
     * product モジュールはシステムの根幹ドメインであり、他モジュールに依存すべきではない。
     * Note: inventory/purchase モジュールの domain が product entity を参照するのは
     *       JPA の ManyToOne 関係として現状許容（将来的にはIDのみ持つよう改善予定）。
     */
    @Test
    void productDomainShouldBeStandalone() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.product.domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..modules.inventory..",
                        "..modules.purchase..",
                        "..modules.customer..",
                        "..modules.backoffice.."
                )
                .because("product.domain層はシステムの根幹であり、他モジュールに依存してはいけません");

        rule.check(classes);
    }

    /**
     * ルール2: product.application.usecase は他モジュールの domain.service を直接参照してはいけない
     *
     * UseCase クラスは他モジュールへのアクセスを Port 経由で行う必要がある。
     * （Adapter層の Controller が BoAuthService を使うのは auth の横断関心事として許容）
     */
    @Test
    void productUsecaseShouldNotAccessOtherModuleDomainService() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.product.application.usecase..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..modules.inventory.domain.service..",
                        "..modules.purchase.cart.service..",
                        "..modules.purchase.order.service..",
                        "..modules.customer.domain.service..",
                        "..modules.backoffice.domain.service.."
                )
                .because("UseCase は他モジュールのサービスを直接参照してはいけません（Port経由で参照してください）");

        rule.check(classes);
    }

    /**
     * ルール3: adapter.rest は自モジュール内の usecase を直接参照してはいけない（Port経由のみ）
     *
     * Controller は application.usecase の実装クラスではなく、application.port のインターフェースを
     * 使用するべきである。
     */
    @Test
    void controllersShouldNotReferenceUseCaseDirectly() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.*.adapter.rest..")
                .should().dependOnClassesThat().resideInAPackage("..modules.*.application.usecase..")
                .because("Controller は UseCase 実装クラスを直接参照してはいけません（Port インターフェース経由で参照してください）");

        rule.check(classes);
    }

    /**
     * ルール4: shared以外のモジュールは他モジュールの application.usecase を参照してはいけない
     *
     * モジュール間の依存は必ず application.port（インターフェース）経由で行う。
     */
    @Test
    void modulesShouldNotAccessOtherModulesUsecaseDirectly() {
        ArchRule rule = noClasses()
                .that().resideOutsideOfPackage("..modules.shared..")
                .should().dependOnClassesThat().resideInAPackage("..modules.*.application.usecase..")
                .because("UseCase 実装クラスはパッケージプライベートであり、他モジュールから直接参照できません（Port経由で参照してください）");

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
                .orShould().resideInAPackage("..modules.*.shipment.entity..") // purchase.shipment 例外
                .orShould().resideInAPackage("..modules.*.outbox.domain.entity..") // shared.outbox 例外
                .orShould().resideInAPackage("..modules.*.job.domain.entity..") // shared.job 例外
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
                .orShould().resideInAPackage("..modules.*.shipment.repository..") // purchase.shipment 例外
                .orShould().resideInAPackage("..modules.*.outbox.domain.repository..") // shared.outbox 例外
                .orShould().resideInAPackage("..modules.*.job.domain.repo..") // shared.job 例外
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
     *
     * Note: Controller内のシンプルなリクエストクラス（inner class）は adapter.rest に
     *       配置されても許容する。
     */
    @Test
    void dtosShouldResideInAdapterDtoPackage() {
        ArchRule rule = classes()
                .that().haveSimpleNameEndingWith("Dto")
                .or().haveSimpleNameEndingWith("Request")
                .or().haveSimpleNameEndingWith("Response")
                .and().areTopLevelClasses()  // inner class（Controller内定義）を除外
                .should().resideInAPackage("..modules.*.adapter.dto..")
                .orShould().resideInAPackage("..modules.shared.dto..")
                .orShould().resideInAPackage("..modules.*.application.port..")
                .because("DTOはadapter.dto / shared.dto / application.portパッケージに配置してください");

        rule.check(classes);
    }

    /**
     * ルール11: domain層はadapter層に依存してはいけない
     */
    @Test
    void domainShouldNotDependOnAdapterLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.*.domain..")
                .should().dependOnClassesThat().resideInAPackage("..modules.*.adapter..")
                .because("domain層はadapter層に依存してはいけません");

        rule.check(classes);
    }

    /**
     * ルール12: application.usecaseはadapter層に依存してはいけない
     */
    @Test
    void usecaseShouldNotDependOnAdapterLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.*.application.usecase..")
                .should().dependOnClassesThat().resideInAPackage("..modules.*.adapter..")
                .because("application.usecase層はadapter層に依存してはいけません");

        rule.check(classes);
    }

    /**
     * ルール13: domain層はapplication層に依存してはいけない
     *
     * エンティティがUseCaseやPortを呼び出す依存逆転を防ぐ。
     */
    @Test
    void domainShouldNotDependOnApplicationLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.*.domain..")
                .should().dependOnClassesThat().resideInAPackage("..modules.*.application..")
                .because("domain層はapplication層に依存してはいけません");

        rule.check(classes);
    }

    /**
     * ルール14: application.portはadapter層に依存してはいけない
     *
     * Portインターフェースの戻り値・引数型はadapter.dtoではなくapplication.port配下に置く。
     * このルールを破るとUseCase実装がadapter.dtoを間接的に参照する構造になる。
     */
    @Test
    void portShouldNotDependOnAdapterLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.*.application.port..")
                .and().areInterfaces()
                .should().dependOnClassesThat().resideInAPackage("..modules.*.adapter..")
                .because("Portインターフェースはadapter層に依存してはいけません（戻り値・引数型はapplication.port配下に置いてください）");

        rule.check(classes);
    }

}
