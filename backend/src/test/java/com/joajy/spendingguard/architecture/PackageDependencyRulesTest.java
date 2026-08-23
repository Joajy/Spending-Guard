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
                        "..infrastructure..",
                        "..controller..",
                        "..service..",
                        "..repository.."
                )
                .check(CLASSES);
    }

    @Test
    void featureServicesDoNotDependOnWebOrPersistenceImplementations() {
        noClasses().that().resideInAPackage("..service..")
                .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..repository..")
                .check(CLASSES);
    }

    @Test
    void featureControllersDoNotDependOnPersistenceImplementations() {
        noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
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
                .should().resideInAnyPackage("..infrastructure.persistence..", "..repository..")
                .check(CLASSES);
    }

    @Test
    void adaptersStayInInfrastructure() {
        classes().that().haveSimpleNameEndingWith("Adapter")
                .should().resideInAnyPackage("..infrastructure..", "..repository..")
                .check(CLASSES);
    }
}
