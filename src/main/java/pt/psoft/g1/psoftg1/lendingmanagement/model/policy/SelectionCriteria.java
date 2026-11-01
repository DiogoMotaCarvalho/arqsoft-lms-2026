package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

/**
 * The validated criteria that govern lending-policy selection (lending spec section 4, issue #12).
 * The values come from the externalized configuration (#9) through the factory. As with the
 * policies, the parameters are validated at construction (fail-fast), so an invalid configuration
 * cannot produce a criteria instance. See ADR-0006.
 *
 * <ul>
 *   <li>{@code historyPeriodInMonths}, the length of the history window, must be positive;
 *   <li>{@code minCompletedLendings}, the minimum number of completed lendings in the window, must
 *       be positive;
 *   <li>{@code minOnTimeRatio}, the minimum on-time ratio as a percentage, must be between 0 and
 *       100.
 * </ul>
 */
public final class SelectionCriteria {

  private final int historyPeriodInMonths;
  private final int minCompletedLendings;
  private final int minOnTimeRatio;

  public SelectionCriteria(
      int historyPeriodInMonths, int minCompletedLendings, int minOnTimeRatio) {
    if (historyPeriodInMonths <= 0) {
      throw new IllegalArgumentException(
          "historyPeriodInMonths must be positive, was " + historyPeriodInMonths);
    }
    if (minCompletedLendings <= 0) {
      throw new IllegalArgumentException(
          "minCompletedLendings must be positive, was " + minCompletedLendings);
    }
    if (minOnTimeRatio < 0 || minOnTimeRatio > 100) {
      throw new IllegalArgumentException(
          "minOnTimeRatio must be between 0 and 100, was " + minOnTimeRatio);
    }
    this.historyPeriodInMonths = historyPeriodInMonths;
    this.minCompletedLendings = minCompletedLendings;
    this.minOnTimeRatio = minOnTimeRatio;
  }

  public int getHistoryPeriodInMonths() {
    return historyPeriodInMonths;
  }

  public int getMinCompletedLendings() {
    return minCompletedLendings;
  }

  public int getMinOnTimeRatio() {
    return minOnTimeRatio;
  }
}
