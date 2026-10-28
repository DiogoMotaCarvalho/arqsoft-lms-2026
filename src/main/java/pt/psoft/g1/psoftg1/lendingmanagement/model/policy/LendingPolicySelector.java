package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.util.Objects;

/**
 * Decides which lending policy applies to a reader, given the reader's history and the selection
 * criteria (issue #12, lending spec section 4). This is a pure, framework-free domain object with
 * no dependency on Spring, the repository, or the factory, so the selection boundaries can be
 * tested at SUT equal to domain. It returns the name of the applicable policy, which the caller
 * resolves to a {@link LendingPolicy}. See ADR-0006.
 *
 * <p>The reader receives the Adaptive policy if and only if the completed count is at least {@code
 * minCompletedLendings}, the on-time ratio is at least {@code minOnTimeRatio}, and the reader has
 * no current overdue lending. Otherwise the reader receives the Standard policy, which is the
 * default.
 */
public final class LendingPolicySelector {

  private final SelectionCriteria criteria;

  public LendingPolicySelector(SelectionCriteria criteria) {
    this.criteria = Objects.requireNonNull(criteria, "criteria is required");
  }

  /** The name of the policy that applies to a reader with the given history. */
  public String selectPolicyName(ReaderLendingHistory history) {
    Objects.requireNonNull(history, "history is required");
    if (isEligibleForAdaptive(history)) {
      return AdaptiveLendingPolicy.NAME;
    }
    return StandardLendingPolicy.NAME;
  }

  private boolean isEligibleForAdaptive(ReaderLendingHistory history) {
    if (history.hasCurrentOverdue()) {
      return false;
    }
    if (history.getCompletedCount() < criteria.getMinCompletedLendings()) {
      return false;
    }
    return onTimeRatio(history) >= criteria.getMinOnTimeRatio();
  }

  /** The on-time ratio as an integer percentage, or 0 when there are no completed lendings. */
  private static int onTimeRatio(ReaderLendingHistory history) {
    if (history.getCompletedCount() == 0) {
      return 0;
    }
    return history.getOnTimeCount() * 100 / history.getCompletedCount();
  }
}
