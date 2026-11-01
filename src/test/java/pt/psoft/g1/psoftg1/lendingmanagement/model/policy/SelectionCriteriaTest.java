package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Opaque-box tests for the fail-fast construction of the selection criteria. Valid values build,
 * and each out-of-range value is rejected.
 */
class SelectionCriteriaTest {

  @Test
  void buildsWithValidValues() {
    SelectionCriteria criteria = new SelectionCriteria(12, 5, 80);
    assertEquals(12, criteria.getHistoryPeriodInMonths());
    assertEquals(5, criteria.getMinCompletedLendings());
    assertEquals(80, criteria.getMinOnTimeRatio());
  }

  @Test
  void acceptsOnTimeRatioAtBothBounds() {
    assertDoesNotThrow(() -> new SelectionCriteria(12, 5, 0));
    assertDoesNotThrow(() -> new SelectionCriteria(12, 5, 100));
  }

  @Test
  void rejectsNonPositiveHistoryPeriod() {
    assertThrows(IllegalArgumentException.class, () -> new SelectionCriteria(0, 5, 80));
  }

  @Test
  void rejectsNonPositiveMinCompletedLendings() {
    assertThrows(IllegalArgumentException.class, () -> new SelectionCriteria(12, 0, 80));
  }

  @Test
  void rejectsOnTimeRatioBelowZero() {
    assertThrows(IllegalArgumentException.class, () -> new SelectionCriteria(12, 5, -1));
  }

  @Test
  void rejectsOnTimeRatioAboveHundred() {
    assertThrows(IllegalArgumentException.class, () -> new SelectionCriteria(12, 5, 101));
  }
}
