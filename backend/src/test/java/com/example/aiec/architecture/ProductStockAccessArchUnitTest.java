package com.example.aiec.architecture;

import com.example.aiec.modules.product.domain.entity.Product;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ProductStockAccessArchUnitTest {

    @Test
    void applicationAndAdapterShouldNotUseProductStockAccessor() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.example.aiec");

        ArchRule rule = noClasses()
                .that().resideInAnyPackage("..modules..application..", "..modules..adapter..")
                .should().callMethod(Product.class, "getStock")
                .orShould().callMethod(Product.class, "setStock", Integer.class)
                .because("products.stock は業務参照禁止であり、allocationType/effectiveStock を利用する必要があります");

        rule.check(classes);
    }
}
