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
                        "..controller..",
                        "..service..",
                        "..repository..",
                        "..messaging..",
                        "..scheduling..",
                        "..config.."
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
    void persistenceEntitiesStayInRepositoryPackages() {
        classes().that().areAnnotatedWith(Entity.class)
                .should().resideInAPackage("..repository..")
                .check(CLASSES);
    }

    @Test
    void adaptersStayInRepositoryPackages() {
        classes().that().haveSimpleNameEndingWith("Adapter")
                .should().resideInAPackage("..repository..")
                .check(CLASSES);
    }
}
