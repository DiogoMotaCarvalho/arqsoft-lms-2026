# Evaluation of the CI/CD process

This document compares the base project's development process with the current one using measured
evidence, identifies the current process's limitations and bottlenecks, and justifies the improvements
introduced. The process is designed in
[`system-to-be/development-process-design.md`](system-to-be/development-process-design.md). The two states
of the system are measured in [`system-as-is/base-project-state.md`](system-as-is/base-project-state.md)
and [`system-current/current-project-state.md`](system-current/current-project-state.md).

**How the evidence was collected.**
- **Stage times.** The CI stages were run in order on a clean copy of the repository, with the same
  commands as `jenkins/Jenkinsfile.ci`, on one developer laptop (JDK 24, Maven wrapper). The times are
  wall-clock times on that machine and vary with load; the ranges come from two runs.
- **The deployment chain.** The smoke test was run against the packaged JAR, started as the systemd
  service starts it.
- **Delivery metrics.** Metrics that need a history of pipeline runs (deployment frequency, lead time,
  failure rate, time to restore) are defined in section 4, with how to collect them from Jenkins.

## 1. Process comparison

| Aspect | Base project | Current project |
|---|---|---|
| Trigger | none: a push runs nothing | every push to any branch runs `psoft-g1-ci` |
| Build | local Maven, broken wrapper, fails on current JDKs | `./mvnw` on any machine, JDK 17 or later |
| Automated checks before an artifact exists | 0 | 7 gates: compile, formatting, Checkstyle, unit tests with architecture rules, integration tests, coverage (60% of lines), mutation (75%) |
| Test levels run automatically | none (tests run only by hand) | unit, application, adapter, contract, architecture, integration, system/API, smoke |
| Reported results | none | test results, Checkstyle issues, coverage and mutation, with HTML reports archived per build |
| Deployable artifact | none kept; built on a laptop | one JAR per `main` build, with its commit embedded, fingerprinted and archived as *validated*, then *accepted* |
| Environments | one implicit (the developer's machine) | DEV, PP (staging) and PROD, created by one script, configured per environment |
| Deployment | `java -jar` by hand, after starting an H2 server by hand | scripted over SSH, with a lock per environment |
| Verification after deployment | none | smoke test: commit and environment identity, readiness, login, authorisation |
| Promotion to production | not applicable | same JAR; manual acceptance in PP; re-check before promotion |
| Manual steps from commit to production | every step (no production existed) | one: the acceptance in PP |
| Rollback | not applicable | re-run PROD with an earlier PP build |

## 2. Measured evidence

### 2.1 Pipeline stage durations (CI, one machine)

| Stage | Duration | Share |
|---|---|---|
| Compile | 19 s | 5% |
| Static analysis (Spotless, Checkstyle) | 14 s | 4% |
| Unit tests (500, including ArchUnit) | 43–77 s | 15% |
| Integration tests (51, JaCoCo agent) | 69–93 s | 20% |
| Coverage report and gate | 5 s | 1% |
| Mutation tests (PIT, 660 mutants) | 186 s | 48% |
| Package | 17 s | 4% |
| **Total** | **about 6 to 7 minutes** | |

A formatting or style failure is reported within about 35 seconds, and a unit-test failure within about
1.5 minutes. The base project had no equivalent: running its 102 tests by hand took 26 seconds, and
nothing else was checked.

After CI, deploying to an environment means copying the JAR, restarting the service and running the smoke
test. Locally, the application was ready after about 10 seconds, and the smoke test passed on its second
check. The smoke test allows up to 150 seconds for start-up.

### 2.2 Outcome metrics

| Metric | Base project | Current project |
|---|---|---|
| Tests (all passing) | 102 | 551 (500 unit, 51 integration) |
| Line coverage, all classes, unit and integration tests | 19.9% | 69.3% |
| Line coverage as gated in CI (project exclusions) | not measured | 67.3% (gate 60%) |
| Mutation score / test strength | 30% / 77% | 80% / 90% (gate 75%) |
| Checkstyle warnings | 5,682 | 0 (gate: any warning fails) |
| New module cycles allowed | not checked | 0 (ArchUnit, versioned baseline of 10 existing cycles) |
| Tests that cannot fail | 2 | 0 |
| Authentication and authorisation tests | 0 | 8 system tests |
| Committed secrets | 3 | 3 |

## 3. Limitations and bottlenecks

**Bottlenecks:**
- **Mutation testing takes half of the pipeline (186 s).** PIT mutates every class of `model` and
  `services` on every push, to every branch.
- **The integration tests start many Spring contexts.** Different `@TestPropertySource` and configuration
  combinations prevent context reuse, so 51 tests take 70–90 seconds.
- **Each stage is a separate `./mvnw` call** that starts Maven and checks the build again, a few seconds
  each.
- **Everything is sequential, on one agent.** Static analysis and the unit tests do not depend on each
  other, but they run one after the other.
- **The manual acceptance in PP** waits for a person, up to 7 days. It dominates the lead time to
  production by design (DP-15).

**Limitations:**
- **No persistent database.** Each environment reseeds an in-memory H2 at start, so PP and PROD cannot
  hold real data, and a restart loses everything.
- **One server for all environments.** A server failure affects DEV, PP and PROD together, and resource
  contention between them is possible.
- **Rollback is manual,** and limited to the 30 builds Jenkins keeps.
- **Secrets remain in the repository.** Only the deployment credentials are in Jenkins.
- **Delivery metrics build up over time.** Deployment frequency, lead time, change failure rate and
  recovery time come from the Jenkins build history as runs accumulate (section 4).
- **The test suite has known gaps:**
  - controllers of the older modules and some custom queries are untested;
  - two security defects inherited from the base project were found by the new tests; their fixes are
    planned (see [`testing-strategy.md`](../testing/testing-strategy.md), section 6).

## 4. Delivery metrics to collect once Jenkins runs

| Metric | Definition | Source |
|---|---|---|
| Deployment frequency | successful `psoft-g1-prod` builds per week | Jenkins build history |
| Lead time for changes | from the commit time (`GIT_COMMIT` in `release.properties`) to the end of the PROD build that deployed it | `release.properties` + `git log` + Jenkins |
| Change failure rate | PROD deployments followed by a failed smoke test or a rollback, divided by all PROD deployments | Jenkins build results |
| Time to restore | from a failed PROD build to the next successful one | Jenkins build history |
| CI duration and failure rate | total and per-stage duration; share of CI builds failing, per stage | Jenkins (Pipeline Stage View) |

These cover the delivery side of the process. Together with the outcome metrics of section 2.2, they
allow the next round of improvements to be judged against evidence.

## 5. Justification of the improvements

| Improvement | Problem it addresses | Evidence |
|---|---|---|
| Working Maven wrapper; Lombok and Byte Buddy upgraded | build only worked on some JDKs, and needed a local Maven | the pipeline commands run on JDK 24 from a clean copy |
| Jenkins CI on every push | nothing ran automatically | 7 gates before any artifact; feedback in 35 s to 7 min |
| Spotless and zero-warning Checkstyle | 5,682 style warnings | 0 warnings, kept at 0 by the gate |
| Separate unit and integration test stages | one mixed phase; slow tests gated fast ones | unit failures reported before any Spring test runs |
| New test levels and tests | 20% coverage, services and API untested | 102 → 551 tests; coverage and mutation in section 2.2 |
| Coverage over unit and integration tests, with a 60% gate | coverage unmeasured; then measured on unit tests only and never enforced | 67.3% measured in CI; a drop below 60% fails the build |
| PIT with a 75% gate | test effectiveness unknown | 80% score, 90% strength |
| ArchUnit rules with a versioned cycle baseline | nothing stopped the structure from eroding; the baseline was git-ignored, so the cycle rule could not fail in CI | rule fails on any new cycle; missing baseline fails the build |
| Results published in Jenkins | results only in logs | Checkstyle, coverage and mutation reports per build |
| Build once, promote by build number, commit embedded | no artifact management | PROD runs the exact JAR accepted in PP; `/actuator/info` shows its commit |
| Three systemd environments, set up by a script | one implicit environment | DEV, PP and PROD created the same way, configured per environment |
| Smoke test after each deployment | no verification after deployment | identity, readiness, login and authorisation checked in each environment |
| Rewritten tests that could not fail; authorisation tests | false confidence; no security tests | 0 vacuous tests; 8 security system tests; 2 defects found |

## 6. Next improvements, in order of expected value

1. **Run PIT incrementally on branches** (only changed classes, with PIT's history), and in full on
   `main`. This removes about half of the branch pipeline time.
2. **Run static analysis and the unit tests in parallel,** and reduce the number of distinct Spring test
   contexts. This shortens feedback.
3. **Move the application secrets out of the repository,** into each environment's `.env` file or
   Jenkins credentials, and rotate them.
4. **Fix the two security defects,** each with the test that reproduces it.
5. **Roll back automatically** to the previous JAR when the smoke test fails after a PROD deployment.
6. **Give PP and PROD a persistent database,** and collect the delivery metrics of section 4 from the
   first weeks of use.
