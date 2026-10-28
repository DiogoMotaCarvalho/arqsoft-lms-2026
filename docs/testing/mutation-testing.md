# Mutation testing (PIT)

Mutation testing with [PIT](https://pitest.org/) measures how well the tests detect faults, rather
than how much code they execute. PIT seeds small faults, called mutants, into the target classes, for
example by changing `<` to `<=` or `>=` to `>`, reruns the tests, and reports each mutant as killed (a
test failed, which is the desired outcome) or survived (no test noticed, which indicates a gap). The
deliverable is the analysis of the survivors, not the score (see ADR-0005).

## Scope

By default PIT targets the lending-policy domain package,
`pt.psoft.g1.psoftg1.lendingmanagement.model.policy`, and its tests, that is, the domain logic with
branch and comparison conditions that mutation testing is intended to probe (criterion C3.3). The
scope is overridable per run, as shown below.

## Prerequisites

1. JDK 17. The shell default may be Java 8, so set `JAVA_HOME` explicitly:
   ```
   export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home
   ```
2. A passing test suite (`./mvnw test`), because PIT only mutates code whose tests currently pass.

## Running

PIT is not part of the normal build, so `./mvnw test` does not run it, which keeps the loop fast. Run
it on demand through the `mutation` profile:

```
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home ./mvnw -Pmutation test
```

Override the scope for a single run with the standard PIT parameters:

```
./mvnw -Pmutation test \
  -DtargetClasses=pt.psoft.g1.psoftg1.lendingmanagement.model.policy.* \
  -DtargetTests=pt.psoft.g1.psoftg1.lendingmanagement.model.policy.*
```

## Reading the report

Reports are written to `target/pit-reports/`, which is non-timestamped so that the path is stable:

1. `index.html`, to be opened in a browser, allows drilling into each class to see the surviving
   mutants line by line.
2. `mutations.xml` is machine-readable and is used to compare results before and after a change.

For each surviving mutant, decide and record which of the following two cases applies.

1. A test gap, that is, a real behaviour that no test pins down. Add or strengthen a test so that the
   mutant is killed, then rerun and show the result before and after.
2. An equivalent mutant, that is, a change that cannot alter observable behaviour, for example a
   mutation on a branch that is unreachable given the constraints of the class. Document the reason it
   is equivalent. These are legitimately left alive.

A high kill score without analysis is not evidence. An analysed set of survivors is.
