package vanilla

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import spock.lang.Shared
import spock.lang.Specification

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices

class ArchitectureSpec extends Specification {

    @Shared
    JavaClasses appClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("vanilla")

    def "controllers must not directly access repositories"() {
        expect:
        noClasses()
                .that().resideInAPackage("vanilla.controller..")
                .should().dependOnClassesThat().resideInAPackage("vanilla.repository..")
                .check(appClasses)
    }

    def "services must not depend on controllers"() {
        expect:
        noClasses()
                .that().resideInAPackage("vanilla.service..")
                .should().dependOnClassesThat().resideInAPackage("vanilla.controller..")
                .check(appClasses)
    }

    def "entities must not depend on services or controllers"() {
        expect:
        noClasses()
                .that().resideInAPackage("vanilla.entity..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("vanilla.service..", "vanilla.controller..")
                .check(appClasses)
    }

    def "DTOs must not depend on services or controllers"() {
        expect:
        noClasses()
                .that().resideInAPackage("vanilla.dto..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("vanilla.service..", "vanilla.controller..", "vanilla.repository..")
                .check(appClasses)
    }

    def "no cyclic dependencies between packages"() {
        expect:
        slices()
                .matching("vanilla.(*)..")
                .should().beFreeOfCycles()
                .check(appClasses)
    }
}
