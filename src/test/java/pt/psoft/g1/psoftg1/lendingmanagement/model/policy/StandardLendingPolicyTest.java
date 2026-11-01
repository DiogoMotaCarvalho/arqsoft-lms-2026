package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Transparent-box and opaque-box tests at SUT = domain class for the Standard policy: decision
 * outcomes and boundary conditions. Parameters: max 3 active lendings, 15-day period, max 3
 * renewals, 15-day renewal extension.
 *
 * <p>Renewal-counting assumption (criterion C3.1): {@code renewalsSoFar} is the number of renewals
 * already performed on the lending, and a renewal is permitted while {@code renewalsSoFar <
 * maxRenewals}. A policy with {@code maxRenewals = N} therefore permits N renewals and denies the
 * (N+1)th, and {@code maxRenewals = 0} permits none. The {@code maxRenewals = 0} partition is
 * exercised below, since the parameter's domain is {@code >= 0} (unlike the strictly-positive
 * parameters).
 */
class StandardLendingPolicyTest {

  private final StandardLendingPolicy policy = new StandardLendingPolicy(3, 15, 3, 15);

  @Test
  void exposesStandardName() {
    assertEquals("standard", policy.name());
  }

  // --- construction / fail-fast validation ---

  @Test
  void rejectsNonPositiveMaxActiveLendings() {
    assertThrows(IllegalArgumentException.class, () -> new StandardLendingPolicy(0, 15, 3, 15));
  }

  @Test
  void rejectsNonPositiveLendingDuration() {
    assertThrows(IllegalArgumentException.class, () -> new StandardLendingPolicy(3, 0, 3, 15));
  }

  @Test
  void rejectsNegativeMaxRenewals() {
    assertThrows(IllegalArgumentException.class, () -> new StandardLendingPolicy(3, 15, -1, 15));
  }

  @Test
  void rejectsNonPositiveRenewalDuration() {
    assertThrows(IllegalArgumentException.class, () -> new StandardLendingPolicy(3, 15, 3, 0));
  }

  @Test
  void acceptsZeroMaxRenewals() {
    // maxRenewals is >= 0 (unlike the strictly-positive parameters), so zero is a valid
    // configuration meaning "no renewals allowed". Asserting that zero is accepted pins the
    // guard's boundary at < 0 rather than <= 0.
    assertDoesNotThrow(() -> new StandardLendingPolicy(3, 15, 0, 15));
  }

  // --- borrow eligibility ---

  @Test
  void allowsBorrowWithNoActiveLendings() {
    assertTrue(policy.canBorrow(new ReaderLendingContext(0, false)).isAllowed());
  }

  @Test
  void allowsBorrowJustBelowTheCap() {
    assertTrue(policy.canBorrow(new ReaderLendingContext(2, false)).isAllowed());
  }

  @Test
  void deniesBorrowAtTheCap() {
    PolicyDecision decision = policy.canBorrow(new ReaderLendingContext(3, false));
    assertTrue(decision.isDenied());
    assertTrue(decision.getReason().orElseThrow().contains("3 books outstanding"));
  }

  @Test
  void deniesBorrowWhileOverdueEvenBelowTheCap() {
    PolicyDecision decision = policy.canBorrow(new ReaderLendingContext(1, true));
    assertTrue(decision.isDenied());
    assertTrue(decision.getReason().orElseThrow().contains("past their due date"));
  }

  // --- lending period ---

  @Test
  void limitDateIsStartPlusFixedPeriod() {
    LocalDate start = LocalDate.of(2026, 1, 10);
    assertEquals(
        start.plusDays(15), policy.limitDateFor(start, new ReaderLendingContext(0, false)));
  }

  @Test
  void limitDateIgnoresActiveLendingCount() {
    LocalDate start = LocalDate.of(2026, 1, 10);
    assertEquals(
        policy.limitDateFor(start, new ReaderLendingContext(0, false)),
        policy.limitDateFor(start, new ReaderLendingContext(2, false)));
  }

  // --- renewal ---

  @Test
  void allowsRenewalBelowTheRenewalCapAndNotOverdue() {
    RenewalContext context = new RenewalContext(LocalDate.of(2026, 1, 10), 2, false);
    assertTrue(policy.canRenew(context).isAllowed());
  }

  @Test
  void deniesRenewalAtTheRenewalCap() {
    RenewalContext context = new RenewalContext(LocalDate.of(2026, 1, 10), 3, false);
    assertTrue(policy.canRenew(context).isDenied());
  }

  @Test
  void deniesAnyRenewalWhenMaxRenewalsIsZero() {
    StandardLendingPolicy noRenewals = new StandardLendingPolicy(3, 15, 0, 15);
    // renewalsSoFar = 0, not overdue: the very first renewal is denied, because 0 >=
    // maxRenewals(0).
    PolicyDecision decision =
        noRenewals.canRenew(new RenewalContext(LocalDate.of(2026, 1, 10), 0, false));
    assertTrue(decision.isDenied());
    assertTrue(decision.getReason().orElseThrow().contains("0 renewals"));
  }

  @Test
  void deniesRenewalWhileOverdue() {
    RenewalContext context = new RenewalContext(LocalDate.of(2026, 1, 10), 0, true);
    PolicyDecision decision = policy.canRenew(context);
    assertTrue(decision.isDenied());
    assertTrue(decision.getReason().orElseThrow().contains("overdue"));
  }

  @Test
  void renewalExtendsLimitDateByRenewalPeriod() {
    LocalDate current = LocalDate.of(2026, 1, 10);
    RenewalContext context = new RenewalContext(current, 0, false);
    assertEquals(current.plusDays(15), policy.renewalLimitDateFor(context));
  }
}
