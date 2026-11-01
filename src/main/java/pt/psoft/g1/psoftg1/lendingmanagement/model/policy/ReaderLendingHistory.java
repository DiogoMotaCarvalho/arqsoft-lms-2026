package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

/**
 * A summary of a reader's recent lending behaviour, consumed by {@link LendingPolicySelector} to
 * decide the applicable policy (issue #12, lending spec section 4). It carries raw counts rather
 * than a pre-computed ratio, which keeps the on-time ratio exact at the selection boundary. See
 * ADR-0006.
 *
 * <ul>
 *   <li>{@code completedCount}, the number of completed lendings within the history window;
 *   <li>{@code onTimeCount}, how many of those were returned on or before their limit date;
 *   <li>{@code hasCurrentOverdue}, whether the reader currently holds any overdue lending.
 * </ul>
 */
public final class ReaderLendingHistory {

  private final int completedCount;
  private final int onTimeCount;
  private final boolean hasCurrentOverdue;

  public ReaderLendingHistory(int completedCount, int onTimeCount, boolean hasCurrentOverdue) {
    if (completedCount < 0) {
      throw new IllegalArgumentException(
          "completedCount cannot be negative, was " + completedCount);
    }
    if (onTimeCount < 0) {
      throw new IllegalArgumentException("onTimeCount cannot be negative, was " + onTimeCount);
    }
    if (onTimeCount > completedCount) {
      throw new IllegalArgumentException(
          "onTimeCount ("
              + onTimeCount
              + ") cannot exceed completedCount ("
              + completedCount
              + ")");
    }
    this.completedCount = completedCount;
    this.onTimeCount = onTimeCount;
    this.hasCurrentOverdue = hasCurrentOverdue;
  }

  public int getCompletedCount() {
    return completedCount;
  }

  public int getOnTimeCount() {
    return onTimeCount;
  }

  public boolean hasCurrentOverdue() {
    return hasCurrentOverdue;
  }
}
