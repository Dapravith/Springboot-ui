package com.springboot.account;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.library.Architectures;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Executable architecture. These rules are the difference between a layered
 * design and a layered diagram: a future change that quietly reaches from the
 * domain into JPA fails the build instead of passing review.
 */
class ArchitectureTest {

    private static final String BASE = "com.springboot.account";

    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void classesWereActuallyImported() {
        // Guards against the classic failure mode of an architecture suite: if the
        // import finds nothing, every rule below passes vacuously and the suite
        // reports green while enforcing nothing at all.
        org.junit.jupiter.api.Assertions.assertFalse(classes.isEmpty(),
                "no classes imported from " + BASE + " - the architecture rules would be meaningless");
    }

    @Test
    void layersRespectTheDependencyRule() {
        Architectures.layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy(BASE + ".domain..")
                .layer("Application").definedBy(BASE + ".application..")
                .layer("Infrastructure").definedBy(BASE + ".infrastructure..")
                .layer("Interfaces").definedBy(BASE + ".interfaces..")

                // Nothing may be depended upon by an inner layer.
                .whereLayer("Interfaces").mayNotBeAccessedByAnyLayer()
                .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure", "Interfaces")
                .check(classes);
    }

    @Test
    void domainDoesNotDependOnSpring() {
        noClasses()
                .that().resideInAPackage(BASE + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework..")
                .because("the domain must stay framework-free so it can be tested and reused without a container")
                .check(classes);
    }

    @Test
    void domainDoesNotDependOnPersistenceOrMessagingTechnology() {
        noClasses()
                .that().resideInAPackage(BASE + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "jakarta.persistence..", "org.hibernate..", "org.apache.kafka..", "tools.jackson..")
                .because("persistence and messaging are infrastructure choices, not domain concepts")
                .check(classes);
    }

    @Test
    void domainDoesNotDependOnTheWebLayer() {
        noClasses()
                .that().resideInAPackage(BASE + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(BASE + ".interfaces..", "jakarta.servlet..")
                .because("the domain must not know how it is being driven")
                .check(classes);
    }

    @Test
    void controllersDoNotReachDirectlyIntoPersistence() {
        noClasses()
                .that().resideInAPackage(BASE + ".interfaces..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".infrastructure.persistence..")
                .because("the web layer must go through a use case, never straight to the database")
                .check(classes);
    }

    @Test
    void persistenceEntitiesDoNotEscapeTheirPackage() {
        noClasses()
                .that().resideOutsideOfPackage(BASE + ".infrastructure.persistence..")
                .should().dependOnClassesThat().haveSimpleName("AccountJpaEntity")
                .because("the JPA entity is a storage detail; the domain model is the currency of the system")
                .check(classes);
    }
}
