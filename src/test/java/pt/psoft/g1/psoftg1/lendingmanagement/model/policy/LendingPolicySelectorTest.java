package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Transparent-box tests (SUT equal to domain class) for the selection decision. Criteria: a minimum
 * of 5 completed lendings and a minimum on-time ratio of 80 per cent. Each condition is exercised
 * at its boundary, and each case asserts the chosen policy name rather than merely covering a
 * branch.
 */
class LendingPolicySelectorTest {

  private static final int MIN_COMPLETED = 5;
  private static final int MIN_RATIO = 80;

  private final LendingPolicySelector selector =
      new LendingPolicySelector(new SelectionCriteria(12, MIN_COMPLETED, MIN_RATIO));

  private String choose(int completed, int onTime, boolean overdue) {
    return selector.selectPolicyName(new ReaderLendingHistory(completed, onTime, overdue));
  }

  // --- completed-count boundary (ratio kept at 100, no overdue) ---

  @Test
  void selectsStandardJustBelowMinCompleted() {
    assertEquals(StandardLendingPolicy.NAME, choose(MIN_COMPLETED - 1, MIN_COMPLETED - 1, false));
  }

  @Test
  void selectsAdaptiveAtMinCompleted() {
    assertEquals(AdaptiveLendingPolicy.NAME, choose(MIN_COMPLETED, MIN_COMPLETED, false));
  }

  @Test
  void selectsStandardWithNoHistory() {
    assertEquals(StandardLendingPolicy.NAME, choose(0, 0, false));
  }

  // --- on-time ratio boundary (100 completed so percentages are exact, no overdue) ---

  @Test
  void selectsStandardJustBelowMinOnTimeRatio() {
    assertEquals(StandardLendingPolicy.NAME, choose(100, 79, false));
  }

  @Test
  void selectsAdaptiveAtMinOnTimeRatio() {
    assertEquals(AdaptiveLendingPolicy.NAME, choose(100, 80, false));
  }

  // --- overdue overrides an otherwise-perfect history ---

  @Test
  void selectsStandardWhenCurrentOverdue() {
    assertEquals(AdaptiveLendingPolicy.NAME, choose(10, 10, false));
    assertEquals(StandardLendingPolicy.NAME, choose(10, 10, true));
  }

  // --- all conditions comfortably met ---

  @Test
  void selectsAdaptiveWhenAllConditionsMet() {
    assertEquals(AdaptiveLendingPolicy.NAME, choose(10, 9, false));
  }
}
