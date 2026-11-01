package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Opaque-box (SUT = class) and transparent-box tests for the Adaptive policy: band selection,
 * borrow/renewal decisions, and fail-fast construction. Parameters: maxActive 5, thresholds 2 and
 * 4, periods long 21 / standard 14 / short 7. Band boundaries are exercised at FirstThreshold-1 /
 * FirstThreshold and SecondThreshold-1 / SecondThreshold; the active-lending cap at
 * MaxActiveLendings-1 / MaxActiveLendings.
 */
class AdaptiveLendingPolicyTest {

  private static final int MAX_ACTIVE = 5;
  private static final int FIRST = 2;
  private static final int SECOND = 4;
  private static final int LONG = 21;
  private static final int STANDARD = 14;
  private static final int SHORT = 7;

  private final AdaptiveLendingPolicy policy =
      new AdaptiveLendingPolicy(MAX_ACTIVE, FIRST, SECOND, LONG, STANDARD, SHORT);

  private static ReaderLendingContext reader(int activeLendings) {
    return new ReaderLendingContext(activeLendings, false);
  }

  @Test
  void exposesAdaptiveName() {
    assertEquals("adaptive", policy.name());
  }

  // --- construction / fail-fast validation ---

  @Test
  void rejectsNonPositiveMaxActiveLendings() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdaptiveLendingPolicy(0, FIRST, SECOND, LONG, STANDARD, SHORT));
  }

  @Test
  void rejectsNonPositiveFirstThreshold() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdaptiveLendingPolicy(MAX_ACTIVE, 0, SECOND, LONG, STANDARD, SHORT));
  }

  @Test
  void rejectsNonPositiveShortPeriod() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdaptiveLendingPolicy(MAX_ACTIVE, FIRST, SECOND, LONG, STANDARD, 0));
  }

  @Test
  void rejectsFirstThresholdNotBelowSecondThreshold() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdaptiveLendingPolicy(MAX_ACTIVE, 4, 4, LONG, STANDARD, SHORT));
  }

  @Test
  void rejectsSecondThresholdNotBelowMaxActiveLendings() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdaptiveLendingPolicy(5, FIRST, 5, LONG, STANDARD, SHORT));
  }

  @Test
  void rejectsLongPeriodNotAboveStandardPeriod() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdaptiveLendingPolicy(MAX_ACTIVE, FIRST, SECOND, 14, 14, SHORT));
  }

  @Test
  void rejectsStandardPeriodNotAboveShortPeriod() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new AdaptiveLendingPolicy(MAX_ACTIVE, FIRST, SECOND, LONG, 7, 7));
  }

  // --- band selection (lending period) + boundaries ---

  @Test
  void usesLongPeriodBelowFirstThreshold() {
    LocalDate start = LocalDate.of(2026, 1, 10);
    assertEquals(start.plusDays(LONG), policy.limitDateFor(start, reader(FIRST - 1)));
  }

  @Test
  void usesStandardPeriodAtFirstThreshold() {
    LocalDate start = LocalDate.of(2026, 1, 10);
    assertEquals(start.plusDays(STANDARD), policy.limitDateFor(start, reader(FIRST)));
  }

  @Test
  void usesStandardPeriodJustBelowSecondThreshold() {
    LocalDate start = LocalDate.of(2026, 1, 10);
    assertEquals(start.plusDays(STANDARD), policy.limitDateFor(start, reader(SECOND - 1)));
  }

  @Test
  void usesShortPeriodAtSecondThreshold() {
    LocalDate start = LocalDate.of(2026, 1, 10);
    assertEquals(start.plusDays(SHORT), policy.limitDateFor(start, reader(SECOND)));
  }

  @Test
  void usesLongPeriodWithNoActiveLendings() {
    LocalDate start = LocalDate.of(2026, 1, 10);
    assertEquals(start.plusDays(LONG), policy.limitDateFor(start, reader(0)));
  }

  // --- borrow eligibility + boundaries ---

  @Test
  void allowsBorrowJustBelowTheCap() {
    assertTrue(policy.canBorrow(reader(MAX_ACTIVE - 1)).isAllowed());
  }

  @Test
  void deniesBorrowAtTheCap() {
    PolicyDecision decision = policy.canBorrow(reader(MAX_ACTIVE));
    assertTrue(decision.isDenied());
    assertTrue(decision.getReason().orElseThrow().contains("books outstanding"));
  }

  @Test
  void deniesBorrowWhileOverdueEvenBelowTheCap() {
    PolicyDecision decision = policy.canBorrow(new ReaderLendingContext(1, true));
    assertTrue(decision.isDenied());
    assertTrue(decision.getReason().orElseThrow().contains("past their due date"));
  }

  // --- renewal: allowed iff not overdue, no cap; period from current band ---

  @Test
  void allowsRenewalWhenNotOverdueRegardlessOfRenewalCount() {
    RenewalContext context = new RenewalContext(LocalDate.of(2026, 1, 10), 99, false, 0);
    assertTrue(policy.canRenew(context).isAllowed());
  }

  @Test
  void deniesRenewalWhileOverdue() {
    RenewalContext context = new RenewalContext(LocalDate.of(2026, 1, 10), 0, true, 0);
    PolicyDecision decision = policy.canRenew(context);
    assertTrue(decision.isDenied());
    assertTrue(decision.getReason().orElseThrow().contains("overdue"));
  }

  @Test
  void renewalExtendsByLongPeriodBelowFirstThreshold() {
    LocalDate current = LocalDate.of(2026, 1, 10);
    RenewalContext context = new RenewalContext(current, 0, false, FIRST - 1);
    assertEquals(current.plusDays(LONG), policy.renewalLimitDateFor(context));
  }

  @Test
  void renewalExtendsByShortPeriodAtSecondThreshold() {
    LocalDate current = LocalDate.of(2026, 1, 10);
    RenewalContext context = new RenewalContext(current, 0, false, SECOND);
    assertEquals(current.plusDays(SHORT), policy.renewalLimitDateFor(context));
  }

  @Test
  void renewalPeriodDependsOnActiveLendingBand() {
    LocalDate current = LocalDate.of(2026, 1, 10);
    LocalDate lowLoad = policy.renewalLimitDateFor(new RenewalContext(current, 0, false, 0));
    LocalDate highLoad = policy.renewalLimitDateFor(new RenewalContext(current, 0, false, SECOND));
    assertNotEquals(lowLoad, highLoad);
  }
}
