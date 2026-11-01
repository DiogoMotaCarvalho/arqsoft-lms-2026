# Mutation analysis of the Adaptive lending policy (issue #10, criterion C3.3)

This document reports the mutation testing performed on the Adaptive lending policy and the analysis
of the mutants that were not killed. The run was produced with the command `./mvnw -Pmutation test`,
which applies PIT to the package `lendingmanagement.model.policy`. The tooling itself is described in
`docs/testing/mutation-testing.md`.

## Summary of the run

| Metric | Value |
|---|---|
| Mutations generated in the policy package | 89 |
| Mutations killed | 80 (90 per cent) |
| Test strength | 94 per cent |
| Surviving mutants analysed below | 9 |

The comparison operators that implement band selection, namely `activeLendings < firstThreshold` and
`activeLendings < secondThreshold` in `periodForActiveLendings`, together with the condition
`activeLendings >= maxActiveLendings` in `canBorrow`, were all killed. The overdue condition and the
renewal condition were likewise killed. No surviving mutant corresponds to a gap in the tests of the
Adaptive decision logic.

## Surviving mutants in AdaptiveLendingPolicy: four equivalent mutants

The four survivors located in `AdaptiveLendingPolicy` are all produced by the Void Method Call
mutator, which removes a call to `requirePositive(...)` in the constructor. The affected fields are
`maxActiveLendings`, `secondThreshold`, `longPeriod`, and `standardPeriod`. Each of these mutants is
equivalent, that is, its removal cannot change the observable behaviour of the constructor for any
input, because a cross-field ordering check already rejects a non-positive value. PIT applies one
mutation at a time, so the complementary positivity guard remains in place in each case.

| Removed guard | Reason the behaviour is unchanged |
|---|---|
| `requirePositive(maxActiveLendings)` | If `maxActiveLendings <= 0`, the check `secondThreshold < maxActiveLendings` is false, because `secondThreshold > firstThreshold > 0`, so the constructor throws. |
| `requirePositive(secondThreshold)` | If `secondThreshold <= 0`, the check `firstThreshold < secondThreshold` is false, because `firstThreshold > 0`, so the constructor throws. |
| `requirePositive(longPeriod)` | If `longPeriod <= 0`, the check `longPeriod > standardPeriod` is false, because `standardPeriod > 0`, so the constructor throws. |
| `requirePositive(standardPeriod)` | If `standardPeriod <= 0`, the check `standardPeriod > shortPeriod` is false, because `shortPeriod > 0`, so the constructor throws. |

The guards on `firstThreshold` and `shortPeriod` are not redundant, because no other condition
guarantees that these two values are positive. The mutants that remove those two guards were killed
by the tests `rejectsNonPositiveFirstThreshold` and `rejectsNonPositiveShortPeriod`.

The four redundant guards are retained rather than removed. They produce a specific per-field message
when a single value is invalid, which is more precise than the message of the cross-field check, and
this is consistent with the fail-fast intent recorded in ADR-0004. The cost of this decision is the
presence of four equivalent mutants, which are documented here rather than pursued with tests that
cannot kill them.

## Surviving mutants outside the scope of this issue

Five further survivors belong to classes delivered by issue #8 and do not form part of the Adaptive
decision logic. They are reported here for completeness and traceability.

The methods `equals`, `hashCode`, and `toString` of `PolicyDecision` account for three of them. These
are value-object methods for which no test currently asserts equality or textual representation. They
are of limited analytical value and would be addressed by a small addition to `PolicyDecisionTest`
under issue #8 rather than under this issue. The remaining two survivors are a conditional-boundary
mutant in `StandardLendingPolicy.requireNonNegative` and a return-value mutant in the same class,
both of which concern Standard-policy construction and fall under issues #8 and #11.
