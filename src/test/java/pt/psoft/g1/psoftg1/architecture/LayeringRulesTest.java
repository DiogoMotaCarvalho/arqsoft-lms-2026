package pt.psoft.g1.psoftg1.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

/**
 * Executable hexagonal-layering rules (ADR-0002). Analyses main classes only; a violation fails the
 * build. Rules the whole codebase already satisfies are enforced globally; the stricter
 * framework-free-domain rule applies only to new domain packages (see the allow-list below).
 */
@AnalyzeClasses(
    packages = "pt.psoft.g1.psoftg1",
    importOptions = ImportOption.DoNotIncludeTests.class)
class LayeringRulesTest {

  /** Controllers must not reach into persistence adapters. */
  @ArchTest
  static final ArchRule api_should_not_depend_on_infrastructure =
      noClasses()
          .that()
          .resideInAPackage("..api..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..infrastructure..");

  /**
   * The domain sits at the bottom: it must not depend on the service, api or infrastructure layers.
   */
  @ArchTest
  static final ArchRule domain_should_not_depend_on_outer_layers =
      noClasses()
          .that()
          .resideInAPackage("..model..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("..services..", "..api..", "..infrastructure..");

  /**
   * New feature domain must be framework-free (no Spring, no JPA). Allow-list, extended per
   * feature. The notification domain (#18) and the lending-event snapshot it consumes are added
   * here, alongside the lending-policy domain (#8). {@code allowEmptyShould} is kept from the first
   * entry's introduction.
   */
  @ArchTest
  static final ArchRule new_domain_must_be_framework_free =
      noClasses()
          .that()
          .resideInAnyPackage(
              "..lendingmanagement.model.policy..",
              "..lendingmanagement.model.events..",
              "..notificationmanagement.model..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage("org.springframework..", "jakarta.persistence..")
          .allowEmptyShould(true);

  /**
   * The notification context may depend on the lending context (it listens for the lending event),
   * but the dependency must stay one-way: the lending context must never depend on the notification
   * context (issue #18, ADR-0014). This keeps the two contexts able to evolve independently and
   * fixes the direction of the single allowed cross-context edge.
   */
  @ArchTest
  static final ArchRule lending_must_not_depend_on_notifications =
      noClasses()
          .that()
          .resideInAPackage("..lendingmanagement..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage("..notificationmanagement..");

  /**
   * The six bounded contexts may reference one another, but must not form dependency cycles. Scoped
   * to the {@code *management} contexts (cross-cutting {@code shared}/{@code auth} excluded). The
   * existing legacy cross-aggregate cycles are grandfathered as a frozen baseline in {@code
   * archunit_store}; no NEW cross-context cycle may be introduced.
   */
  @ArchTest
  static final ArchRule contexts_should_be_free_of_cycles =
      FreezingArchRule.freeze(
          slices().matching("pt.psoft.g1.psoftg1.(*)management..").should().beFreeOfCycles());
}
