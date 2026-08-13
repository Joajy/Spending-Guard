package com.joajy.spendingguard.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PackageDependencyRulesTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.joajy.spendingguard");

    @Test
    void domainDoesNotDependOnOuterLayers() {
        noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..api..",
                        "..application..",
                        "..infrastructure.."
                )
                .check(CLASSES);
    }

    @Test
    void applicationDoesNotDependOnApiOrInfrastructure() {
        noClasses().that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage("..api..", "..infrastructure..")
                .check(CLASSES);
    }

    @Test
    void apiDoesNotDependOnInfrastructure() {
        noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .check(CLASSES);
    }

    @Test
    void persistenceEntitiesStayInInfrastructure() {
        classes().that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..infrastructure.persistence..")
                .check(CLASSES);
    }

    @Test
    void adaptersStayInInfrastructure() {
        classes().that().haveSimpleNameEndingWith("Adapter")
                .should().resideInAPackage("..infrastructure..")
                .check(CLASSES);
    }
}
