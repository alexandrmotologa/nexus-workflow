package com.engine.nexus;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void setup() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.engine.nexus");
    }

    @Test
    @DisplayName("Domain layer must be pure: zero dependencies on Spring, Hibernate, Jackson or Infrastructure")
    void domainMustBeIndependent() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "com.fasterxml.jackson..",
                        "..infrastructure..",
                        "..application.."
                )
                .because("Domain layer must remain strictly pure and framework-agnostic")
                .check(importedClasses);
    }

    @Test
    @DisplayName("Application layer must not depend on Infrastructure adapters")
    void applicationMustNotDependOnInfrastructure() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("Application layer orchestrates domain models without coupling to concrete adapters")
                .check(importedClasses);
    }
}
