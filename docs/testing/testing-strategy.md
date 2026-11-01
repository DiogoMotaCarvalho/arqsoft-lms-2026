# Automated testing strategy

This document assesses the base project's testing approach and defines the automated testing strategy of
the current project: the levels and scopes of testing, what each level is for, how test doubles are used,
where each level runs, and the targets that the pipeline enforces. The resulting numbers are in the
[current project report](../lmRedesign/system-current/current-project-state.md), section 3.

## 1. Assessment of the base project's approach

The [base project report](../lmRedesign/system-as-is/base-project-state.md), sections 2 and 3, records
the details. In short:

- **Levels:** only domain unit tests had substance: 89 of 102 tests. 13 Spring tests started the
  context, two repositories and two services. API tests were written but commented out, and the API was
  checked by hand with Postman.
- **Scope:** coverage was 19.9% of lines. The application services (7.8%), the REST layer (1.9%), the
  custom queries, security and the external integration were essentially untested.
- **Effectiveness:** the mutation score was 30%, but test strength was 77%. The tests that existed mostly
  checked what they ran; they just ran very little. Two tests could never fail.
- **Organisation:** one Surefire phase for everything, inconsistent names, Mockito in one class, no
  automated run anywhere.

The problem was therefore not the quality of the existing tests but missing levels and missing scope,
with nothing measuring either.

## 2. Principles

- **Test behaviour at the highest seam that still isolates it.** A rule is tested where it lives:
  - a lending policy, on the policy;
  - a use case, on the application service;
  - an endpoint, through HTTP.

  Tests assert outcomes, not calls.
- **Classicist test doubles.** Real objects wherever possible. Fakes replace collaborators that need
  infrastructure (in-memory repositories); stubs replace systems outside the process (catalogue and
  notification gateways). Mocks are the exception, not the default.
- **Every level runs automatically.** Nothing depends on a manual run; the pipeline stage for each level
  is listed below.
- **Effectiveness is measured, not assumed.** Mutation testing checks that the tests detect faults;
  coverage only checks that code runs.

## 3. Levels and scopes

| Level | Scope (system under test) | Doubles | Naming / runner | Pipeline stage | Examples |
|---|---|---|---|---|---|
| Domain unit | one entity, value object, policy or selector | none | `*Test`, Surefire | Unit tests | `LendingTest`, `AdaptiveLendingPolicyTest`, `LendingPolicySelectorTest` |
| Application unit | one application service with its real domain | fake in-memory repositories | `*Test`, Surefire | Unit tests | `LendingServiceImplTest`, `CompositeNotifierTest` |
| Adapter unit | one provider adapter or notification channel | stubbed HTTP server or gateway | `*Test`, Surefire | Unit tests | `GoogleBooksBibliographicProviderTest`, `WebhookNotificationChannelTest` |
| Contract | every implementation of a port, against the same expectations | the adapters' stubs | `*ContractTest`, Surefire | Unit tests | `BibliographicProviderContractTest` |
| Architecture | the whole code base's dependencies | none | `LayeringRulesTest` (ArchUnit), Surefire | Unit tests | layering, framework-free domain, module cycles |
| Integration | Spring wiring with the database: repositories, services, events, seeder | H2 in memory | `*IT`, Failsafe | Integration tests | `LendingRepositoryIT`, `NotificationTriggerIT`, `BootstrapperIT` |
| System / API | a feature through the REST API: controller, security, services, persistence | external systems stubbed | `*SystemApiIT`, `*ComponentIT`, Failsafe | Integration tests | `LendingRenewalSystemApiIT`, `NotificationFanoutSystemApiIT`, `AuthenticationAndAuthorisationSystemApiIT` |
| Smoke | the deployed JAR in its environment | none | `deploy/smoke-test.sh` | after each deployment (DEV, PP, PROD) | identity, readiness, login, authorisation |
| Mutation | the `model` and `services` packages, against the unit tests | as above | PIT | Mutation tests | 75% threshold |

The unit levels give fast feedback and run first. The Spring-based levels run only once the unit tests
pass. The smoke test covers what no earlier level can: the real configuration on the real server.

### 3.1 Mapping to the required kinds of functional tests

| Required kind | Tests in the current project (all on functionality developed in this project, except the security test) |
|---|---|
| 1. Opaque-box, SUT = classes | `OpenLibraryBibliographicProviderTest`, `GoogleBooksBibliographicProviderTest` (adapter classes, through their port); `LendingPolicyPropertiesTest` (configuration binding); `CompositeNotifierTest`; the channel tests |
| 2. Transparent-box, SUT = domain classes | `StandardLendingPolicyTest`, `AdaptiveLendingPolicyTest` (every branch and band boundary), `LendingPolicySelectorTest`, `SelectionCriteriaTest`, `BibliographicDataTest`, `NotificationRendererTest`, `LendingEventTest` |
| 3. Mutation, SUT = classes | PIT on every `model` and `services` class: 80% killed, 90% test strength. The survivors in the policy package are analysed in [`mutation-analysis-adaptive-policy.md`](mutation-analysis-adaptive-policy.md) |
| 4. Opaque-box, SUT = controller + service + {domain, repository, gateways} | `LendingRenewalComponentIT` (controller, service, policies, repository); `BookEnrichmentComponentIT` (controller, service, selecting provider, stubbed gateways) |
| 5. Opaque-box, SUT = system | `LendingRenewalSystemApiIT`, `BookEnrichmentSystemApiIT`, `BookEnrichmentDisabledApiIT`, `NotificationFanoutSystemApiIT`, `NotificationDisabledSystemApiIT`, `AuthenticationAndAuthorisationSystemApiIT` (full Spring context through HTTP); `deploy/smoke-test.sh` on each deployed environment |
| 6. Focus on this project's functionality | lending policies and renewal, bibliographic enrichment and fallback, and notifications have tests at every level above |
| 7. Quality and representativeness over quantity | tests assert outcomes through public behaviour, use real objects and fakes rather than mocks, and are judged by mutation score and test strength rather than line coverage |

## 4. Targets enforced by the pipeline

| Measure | Target | Where |
|---|---|---|
| All tests pass | 100% | Unit tests and Integration tests stages |
| Architecture rules | 0 violations, 0 new cycles | Unit tests stage (ArchUnit) |
| Line coverage, unit and integration tests together | at least the JaCoCo gate in `pom.xml` | Coverage stage |
| Mutation score on `model` and `services` | at least 75% | Mutation tests stage |
| Smoke test | passes in every environment | each deployment |

## 5. What the strategy changed in the suite

- **Missing levels were added:** application, adapter, contract, architecture, system/API and smoke.
- **Unit and integration tests were separated** by name and by runner (`3eea966`).
- **The base project's domain tests were rewritten** around individual business rules (`f4f1586`).
- **The application services got unit tests** with fake repositories (`ffbdb79`).
- **The tests that could never fail were rewritten** to assert real outcomes:
  - `PsoftG1ApplicationIT` now checks that every module's REST API is mapped;
  - `AuthorServiceImplIT` now goes through the real repository instead of a mock stubbed on the wrong
    method.
- **Authentication and authorisation now have system tests** (`AuthenticationAndAuthorisationSystemApiIT`):
  - login, including a wrong password;
  - an anonymous request;
  - access by role;
  - a reader seeing only their own lendings.

## 6. Known gaps and next steps

- **Untested REST layer.** The controllers of Authors, Readers, Genres and user administration are barely
  exercised, and the Genre, Reader and User custom queries are untested. The next system tests should
  target them.
- **Two security defects in the application,** found while writing the authorisation tests and not yet
  fixed:
  - **Shadowed role rules.** In `SecurityConfig`, `GET /api/books/{isbn}` is declared before
    `/api/books/top5` and `/api/books/suggestions`, and `GET /api/authors/{authorNumber}` before
    `/api/authors/top5`. The more specific role rules therefore never apply.
  - **Registration hashes the password twice,** so a self-registered account cannot log in.

  Each fix should come with a test that reproduces the defect.
- **Mutation testing doesn't use the integration tests.** PIT runs against the unit tests only, so code
  that only the integration tests reach counts as not covered.
- **Mockito remains in four unit-test classes,** where a fake would fit the classicist approach better:
  - `LendingEventTest` mocks `Lending`, `ReaderDetails` and `Reader`;
  - `OverdueNotificationSchedulerTest` mocks `LendingRepository`;
  - `CompositeNotifierTest` and `CompositeNotifierSelectionConfigTest` mock the audit repository and
    verify its `save` calls.
