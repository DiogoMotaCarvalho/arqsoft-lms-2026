# Mutation testing (PIT)

Mutation testing with [PIT](https://pitest.org/) measures how well our tests *detect* faults, not just
how much code they execute. PIT seeds small faults ("mutants") into the target classes — e.g. flipping
`<` to `<=` or `>=` to `>` — reruns the tests, and reports each mutant as **killed** (a test failed,
good) or **survived** (no test noticed, a gap). The deliverable here is the **survivor analysis**, not
the score (see ADR-0005).

## Scope

By default PIT targets the lending-policy domain package
(`pt.psoft.g1.psoftg1.lendingmanagement.model.policy`) and its tests — the domain logic with
branch/comparison conditions that mutation testing is meant to probe (rubric C3.3). This is overridable
per run (see below).

## Prerequisites

- JDK 17. The shell default may be Java 8, so set `JAVA_HOME` explicitly:
  ```
  export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
  ```
- A green test suite (`./mvnw test`) — PIT only mutates code whose tests currently pass.

## Running

PIT is **not** part of the normal build (`./mvnw test` does not run it, to keep the loop fast). Run it
on demand via the `mutation` profile:

```
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./mvnw -Pmutation test
```

Override the scope for a one-off run (standard PIT parameters):

```
./mvnw -Pmutation test \
  -DtargetClasses=pt.psoft.g1.psoftg1.lendingmanagement.model.policy.* \
  -DtargetTests=pt.psoft.g1.psoftg1.lendingmanagement.model.policy.*
```

## Reading the report

Reports are written to `target/pit-reports/` (non-timestamped, so the path is stable):

- `index.html` — open in a browser; drill into each class to see surviving mutants line-by-line.
- `mutations.xml` — machine-readable, for diffing before/after.

For each **surviving** mutant, decide and record which it is:

1. **A test gap** — a real behaviour no test pins down. Add/strengthen a test so the mutant is killed,
   then rerun and show the before/after.
2. **An equivalent mutant** — a change that cannot alter observable behaviour (e.g. a mutation on a
   branch that is unreachable given the class's constraints). Document *why* it is equivalent; these are
   legitimately left alive.

A high kill score with no analysis is not evidence; an analysed set of survivors is.
