# ASR catalog: quality-attribute scenarios

This document is the catalog of Architecturally Significant Requirements for the Library Management
System redesign. It turns the three architectural drivers named in the problem statement (section 1.2),
namely Extensibility, Configurability, and Testability, into prioritized, measurable quality-attribute
scenarios that drive the ADD process and the architecture decisions recorded in `docs/adr`.

Each driver is expressed as one six-part quality-attribute scenario (source, stimulus, artifact,
environment, response, and response measure) with a priority. The catalog then records one evolution
scenario per variability axis (lending policy, bibliographic source, and notification channel), each
stating what "add one with limited impact" means and the criterion by which the impact is judged.
Response measures are concrete and checkable, and they are grounded in work already delivered so that
each can be confirmed against the codebase rather than merely asserted. A traceability section links
every requirement to the tickets, decision records, and tests that satisfy it. See ADR-0009 for the
methodology and the alternatives considered.

This is a living document. It is kept consistent with the delivered system as tickets land.

## Drivers and variability axes

| Driver | What it means here | Primary variability axis |
|---|---|---|
| Extensibility | A new variant of a varying feature is added with limited, localized impact on existing code. | Lending policy |
| Configurability | A quantitative parameter changes through external configuration, without a code edit or a rebuild. | Lending-policy parameters and bibliographic configuration |
| Testability | A decision rule is verifiable at a high seam, with no infrastructure, asserting outcomes. | The framework-free policy domain |

## Prioritization

Requirements are ranked with a lightweight utility-tree style, each tagged with a business importance
and an architectural difficulty on a High, Medium, or Low scale. The three drivers are all mandated by
the problem statement, so all three are High importance, and architectural difficulty is the
tie-breaker that orders the design work.

| ASR | Importance | Difficulty | Rationale |
|---|---|---|---|
| Extensibility (ASR-1) | High | Medium | The lending policy and the bibliographic source both vary, so the seam that isolates them is central to the redesign and carries the most design risk. |
| Testability (ASR-3) | High | Medium | A framework-free domain and a high test seam must be designed for, not retrofitted, and the mutation target raises the bar beyond line coverage. |
| Configurability (ASR-2) | High | Low | Externalized, startup-validated configuration is a well-understood Spring mechanism, so the importance is high but the difficulty is low. |

## Driver scenarios

### ASR-1. Extensibility (High, Medium)

- **Source.** A developer extending the system.
- **Stimulus.** A new lending policy must be supported.
- **Artifact.** The lending-policy module, that is, the Strategy port and its implementations.
- **Environment.** Design and development time, on the main branch with the test suite in place.
- **Response.** The new policy is added behind the existing lending-policy port and is selected through
  configuration, without altering the existing policies.
- **Response measure.** The policy is added by creating one new policy class, one factory branch, and
  one configuration section, modifying zero existing policy classes, leaving the existing policy tests
  unchanged, and keeping the ArchUnit layering rules green.

### ASR-2. Configurability (High, Low)

- **Source.** A system administrator or operator.
- **Stimulus.** A quantitative parameter of a lending policy must change, for example the maximum
  number of active lendings or the lending period.
- **Artifact.** The externalized lending-policy and bibliographic configuration.
- **Environment.** Deployment and startup, without a rebuild.
- **Response.** The value changes through external configuration and is validated before the
  application serves requests.
- **Response measure.** The change edits only external configuration, that is, a properties file, an
  environment variable, or a launch argument. It requires zero source edits and zero recompilation, and
  an invalid value is rejected at startup rather than at first use.

### ASR-3. Testability (High, Medium)

- **Source.** A developer or an assessor.
- **Stimulus.** A lending-policy decision rule must be verified, for example the borrow-eligibility
  rule or the band-period selection.
- **Artifact.** The framework-free lending-policy domain.
- **Environment.** The automated test suite, with no network and no database.
- **Response.** The rule is exercised at the highest seam that still isolates it, and the test asserts
  the decision outcome rather than the execution path.
- **Response measure.** Each decision is covered by opaque-box and transparent-box tests with the
  system under test equal to the domain, the mutation kill on the policy conditions is at least 90 per
  cent, and the tests run under a plain `./mvnw test` with no external dependency.

## Evolution scenarios

Each evolution scenario is a limited-impact scenario that this catalog authors, together with the
criterion by which the impact is judged. This makes "limited impact" a defined, verifiable target that
we own, rather than a vague goal.

### EVO-1. Add a lending policy

- **Scenario.** A developer must add a new lending policy, for example a seasonal or a membership-tier
  policy, alongside the Standard and Adaptive policies.
- **Impact criterion.** The policy is added by creating one new policy class and one factory branch and
  one configuration section, modifying zero existing policy classes, with the existing policy tests
  unchanged and the ArchUnit layering rules green.
- **Status.** Demonstrated. The Adaptive policy was added this way behind the Strategy port, with no
  change to the Standard policy.

### EVO-2. Add a bibliographic source

- **Scenario.** A developer must add a third bibliographic source alongside Google Books and Open
  Library.
- **Impact criterion.** The source is added by creating one new adapter that implements the
  bibliographic provider port plus its own configuration, with zero changes to the port or to the Book
  flow, and selection picks it up by configuration.
- **Status.** Enabled by the bibliographic provider port, realized by the Google Books and Open Library
  adapters and by source selection.

### EVO-3. Add a notification channel (hypothetical)

- **Scenario.** A developer must add a notification channel, for example email or SMS, so that readers
  are notified of events such as an overdue lending.
- **Impact criterion.** Were notifications introduced behind a channel port, a new channel would be
  added by creating one new adapter plus its configuration, with zero changes to the code that raises
  notifications.
- **Status.** Forward-looking. No notification feature exists in the system yet. The scenario is
  recorded so that the axis has a stated impact budget and the same ports-and-adapters shape as the
  other two axes, and so that the design is judged against it if notifications are built.

## Traceability

Each requirement links to the tickets, decision records, and evidence that satisfy it. Diagram and
view tickets (#7, #14, #28) and the full versioned matrix (#29) are tracked separately.

| ASR or scenario | Tickets | Decision records | Evidence |
|---|---|---|---|
| ASR-1 Extensibility | #8, #12, #13, #2 | ADR-0001, ADR-0006, ADR-0007, ADR-0008 | The lending-policy Strategy seam and its extensions, and the bibliographic provider port. |
| ASR-2 Configurability | #9 | ADR-0003 | The externalized, startup-validated lending-policy parameters. |
| ASR-3 Testability | #17, #30, #8, #10, #12, #13 | ADR-0005, ADR-0002 | The PIT mutation tooling, the ArchUnit layering enforcement, and the policy tests. |
| EVO-1 Add a lending policy | #10 | ADR-0004 | The Adaptive policy added behind the Strategy port. |
| EVO-2 Add a bibliographic source | #2, #3, #4, #5 | ADR-0008 | The provider port and its adapters and selection. |
| EVO-3 Add a notification channel | none | none | Hypothetical axis, recorded for impact budgeting. |

## Revisit conditions

The catalog is revisited if a new variability axis appears, if a driver's measure proves unachievable
or too weak in practice, or if a notification feature is introduced, at which point EVO-3 is grounded
in real artifacts.
