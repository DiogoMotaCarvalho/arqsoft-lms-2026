# Mutation analysis of the Standard lending policy (issue #11, criterion C3.3)

This document reports the mutation testing performed on the Standard lending policy and the analysis
of the mutants that were not killed. The run was produced with the command `./mvnw -Pmutation test`,
which applies PIT to the package `lendingmanagement.model.policy`. The tooling itself is described in
`docs/testing/mutation-testing.md`. The Standard policy code and most of its tests were delivered by
issues #8 and #9, so this analysis closes the one remaining piece of #11, the C3.3 evidence.

## Summary of the run, before and after

The analysis found one real test gap in `StandardLendingPolicy` and closed it with one boundary test,
so the before and after are reported together.

| Metric | Before | After |
|---|---|---|
| Mutations generated in the policy package | 125 | 125 |
| Mutations killed | 116 (93 per cent) | 117 (94 per cent) |
| Test strength | 96 per cent | 97 per cent |
| Surviving mutants in `StandardLendingPolicy` | 1 | 0 |

Every mutant in `StandardLendingPolicy` is now killed. The remaining non-killed mutants from the run
all belong to other classes and are outside the scope of this issue, as recorded at the end.

## The surviving mutant in StandardLendingPolicy, a real test gap

The single survivor was produced by the Conditionals Boundary mutator in `requireNonNegative`, the
guard applied to `maxRenewals` in the constructor. The mutator changes the boundary `value < 0` into
`value <= 0`.

`maxRenewals` is the only parameter whose domain is non-negative rather than strictly positive, since
a value of zero is a valid configuration meaning that no renewal is allowed. The original guard
therefore accepts zero and the mutated guard rejects it, so the two differ for exactly one input, the
value zero. The tests before this issue only asserted that a negative value is rejected, using `-1`,
which both the original and the mutant reject, so the mutant survived. This is a genuine test gap, not
an equivalent mutant.

The gap was closed by two cases added to `StandardLendingPolicyTest`:

| Test | What it pins |
|---|---|
| `acceptsZeroMaxRenewals` | `new StandardLendingPolicy(3, 15, 0, 15)` does not throw, which is the one input that distinguishes `< 0` from `<= 0` and so kills the boundary mutant. |
| `deniesAnyRenewalWhenMaxRenewalsIsZero` | With a zero cap, the first renewal (`renewalsSoFar = 0`, not overdue) is denied, which asserts the behavioural consequence of the boundary rather than only the construction. |

The renewal-counting assumption this rests on is now stated in the test class documentation:
`renewalsSoFar` is the number of renewals already performed, a renewal is permitted while
`renewalsSoFar < maxRenewals`, so `maxRenewals = N` permits N renewals and denies the N plus first,
and `maxRenewals = 0` permits none.

## Surviving mutants outside the scope of this issue

The remaining non-killed mutants from the same run do not belong to the Standard decision logic and
are reported here only for traceability. They are analysed under their owning issues.

1. Four survivors in `AdaptiveLendingPolicy`, all from the Void Method Call mutator removing a
   `requirePositive(...)` call in the constructor. These are the equivalent mutants already analysed
   in `docs/testing/mutation-analysis-adaptive-policy.md` under issue #10, where a cross-field
   ordering check makes each removed guard behaviourally redundant. They are not pursued here.
2. Four mutants with no coverage in `PolicyDecision`, on the value-object methods `equals`, `hashCode`,
   and `toString`. These are of limited analytical value and belong to the `PolicyDecision` value
   object delivered by issue #8, where a small addition to `PolicyDecisionTest` would address them.
   They are not part of the Standard policy decision logic.

## Conclusion

The Standard lending policy has no surviving mutant after this issue. The comparison operators and
conditions that implement its rules, namely the overdue and active-lending checks in `canBorrow`, the
overdue and renewal-cap checks in `canRenew`, the fixed-period computations, and the construction
guards, are all killed, which is the C3.3 evidence for issue #11. The one gap the analysis found, the
unchecked `maxRenewals = 0` boundary, was a real omission and is now covered.
