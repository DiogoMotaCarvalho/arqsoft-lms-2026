# Traceability: requirements, decisions, views, implementation and tests

Each row links a requirement to the design decision that addresses it, the views that show the decision,
the code that implements it, and the tests that verify it.
- **Requirements** come from the [ASR catalog](asr-catalog.md) (ASR, EVO) and the drivers of the
  [ADD record](add.md) (D1–D5).
- **Decisions** are the ADD iterations (It.) and the process decisions of the
  [process design](../lmRedesign/system-to-be/development-process-design.md) (DP).
- **Views** are the current project's C4 views
  ([`lms-current-c4-views.drawio`](lms-current-c4-views.drawio)) and the ADD views
  ([`add-views.drawio`](add-views.drawio)).

## Architectural requirements

| Requirement | Decision | Views | Implementation | Verified by |
|---|---|---|---|---|
| ASR-1 Extensibility, EVO-1 add a lending policy (D1) | It. 1: `LendingPolicy` Strategy + factory + per-reader selector | ADD view 1; C4 L3 Lendings | `lendingmanagement.model.policy.*`, `LendingPolicyFactory`, `SelectingLendingPolicyProvider` | `StandardLendingPolicyTest`, `AdaptiveLendingPolicyTest`, `LendingPolicySelectorTest`, `LendingPolicyFactoryTest`; Adaptive added without changing Standard (`6a268aa`) |
| ASR-2 Configurability (D1) | It. 1: validated `lending.policy.*` properties; DP-16 | ADD view 1 | `LendingPolicyProperties`, `application.properties`, `/etc/psoft-g1/<env>.env` | `LendingPolicyPropertiesTest` (invalid values fail at startup) |
| ASR-3 Testability (D5) | It. 5: framework-free domain, ArchUnit rules, fakes and stubs, PIT; DP-7–DP-10 | ADD view 5 | `LayeringRulesTest`, `archunit_store/`, PIT and JaCoCo in `pom.xml` | PIT 94% on `model`, 80% overall; ArchUnit on every build |
| EVO-2 add a bibliographic source (D2) | It. 2: `BibliographicProvider` port + one adapter per source | ADD view 2; C4 L3 Books; C4 L1/L2 external systems | `BibliographicProvider`, `GoogleBooksBibliographicProvider`, `OpenLibraryBibliographicProvider` | `BibliographicProviderContractTest`; Open Library added in its own package only (`8b84dfe`) |
| Availability on provider failure (D3) | It. 3: `SelectingBibliographicProvider` fallback, timeouts, best-effort enrichment | ADD view 3; C4 L3 Books | `SelectingBibliographicProvider`, `BibliographicEnrichmentService`, `BookController.findByIsbn` | `SelectingBibliographicProviderTest`, `BookEnrichmentComponentIT`, `BookEnrichmentSystemApiIT`, `BookEnrichmentDisabledApiIT` |
| EVO-3 add a notification channel, failure isolation (D4) | It. 4: lending events after commit, `Notifier` / `NotificationChannel`, composite, audit | ADD view 4; C4 L3 Notifications; C4 L2 Notification Module | `notificationmanagement.*`, `LendingEvent`, `OverdueNotificationScheduler` | `CompositeNotifierTest`, channel tests, `NotificationTriggerIT`, `NotificationFanoutSystemApiIT`, `OverdueSweepDeliveryIT` |
| One-way Lending → Notifications dependency | It. 4 | C4 L2; ADD view 4 | package dependencies | ArchUnit rule `lending_must_not_depend_on_notifications` |
| No new module cycles; layering | It. 5 | C4 L2 (cycles), C4 L3 (red violations) | `LayeringRulesTest`, `archunit_store/` | ArchUnit, on every build |

## Functional requirements of the current project

| Requirement | Decision | Implementation | Verified by |
|---|---|---|---|
| Standard and Adaptive lending policies | It. 1 | `StandardLendingPolicy`, `AdaptiveLendingPolicy` | transparent-box policy tests, PIT |
| Policy chosen per reader from history | It. 1 | `LendingPolicySelector`, `SelectingLendingPolicyProvider` | `LendingPolicySelectorTest`, `SelectingLendingPolicyProviderTest` |
| Renew a lending | It. 1 | `PATCH /api/lendings/{year}/{seq}/renew` | `LendingRenewalComponentIT`, `LendingRenewalSystemApiIT` |
| Enrich a book from external catalogues | It. 2, It. 3 | `GET /api/books/{isbn}` | `BookEnrichment*IT` |
| Notify readers of lending events | It. 4 | `LendingEventListener`, channels | `NotificationTriggerIT`, `NotificationFanoutSystemApiIT`, `NotificationDisabledSystemApiIT` |
| Record each delivery attempt | It. 4 | `NotificationRecord` | `CompositeNotifierTest` |

## Process requirements

| Requirement | Decision | Implementation | Verified by |
|---|---|---|---|
| Build, analyse and test every push | DP-1, DP-6, DP-7 | `jenkins/Jenkinsfile.ci` | CI stages; local run of every stage on a clean copy |
| Coverage and mutation, with gates | DP-9, DP-10 | Coverage and Mutation stages; `pom.xml` | 67.3% lines (gate 60%); 80% mutation (gate 75%) |
| Reporting of results | DP-11 | `recordIssues`, `recordCoverage`, `junit`, archived reports | Jenkins build pages |
| One artifact, promoted | DP-2–DP-4 | `Jenkinsfile.pp`, `Jenkinsfile.prod`, `build-info` | `/actuator/info` commit checked by the smoke test |
| Repeatable deployment to DEV, PP, PROD | DP-12, DP-13, DP-16 | `deploy/` | `deploy/smoke-test.sh` after each deployment |
| Production needs acceptance | DP-15 | `input` step in `Jenkinsfile.pp` | Jenkins approval record (`acceptance.properties`) |

## Consistency checks

- **The C4 views match the code.** The current project's views were derived from the imports of the
  measured commit. Violations still present in the code are drawn in red; those fixed since the base
  project are noted on the L3 pages.
- **The ADD decisions match the commits.** Each ADD decision names the commits that implement it, and
  each evaluation step states what was verified (by history or tests) and what is only plausible.
- **Two known inconsistencies with the intended design remain in the code:**
  - the shadowed role rules in `SecurityConfig`;
  - `CompositeNotifier` depending on its module's infrastructure package.

  Both are recorded in the [testing strategy](../testing/testing-strategy.md) and the
  [ADD record](add.md), iteration 4.
