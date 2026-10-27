package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.time.LocalDate;

/**
 * The Standard lending policy: fixed limits and a fixed lending period, independent of the reader's
 * situation. This is the reference implementation of the {@link LendingPolicy} seam and the policy
 * returned by the default provider stub until the selection algorithm (#12) exists.
 *
 * <p>Rules:
 *
 * <ul>
 *   <li>a reader may not borrow while any active lending is overdue;
 *   <li>a reader may not hold more than {@code maxActiveLendings} active lendings at once;
 *   <li>the lending period is a fixed {@code lendingDurationInDays};
 *   <li>a lending may not be renewed while overdue, nor more than {@code maxRenewals} times;
 *   <li>a renewal extends the limit date by a fixed {@code renewalDurationInDays}.
 * </ul>
 *
 * <p>All parameters are validated at construction (fail-fast); an invalid configuration cannot
 * produce a policy instance.
 */
public final class StandardLendingPolicy implements LendingPolicy {

  /** Stable identifier of this policy. */
  public static final String NAME = "standard";

  private final int maxActiveLendings;
  private final int lendingDurationInDays;
  private final int maxRenewals;
  private final int renewalDurationInDays;

  public StandardLendingPolicy(
      int maxActiveLendings,
      int lendingDurationInDays,
      int maxRenewals,
      int renewalDurationInDays) {
    this.maxActiveLendings = requirePositive(maxActiveLendings, "maxActiveLendings");
    this.lendingDurationInDays = requirePositive(lendingDurationInDays, "lendingDurationInDays");
    this.maxRenewals = requireNonNegative(maxRenewals, "maxRenewals");
    this.renewalDurationInDays = requirePositive(renewalDurationInDays, "renewalDurationInDays");
  }

  @Override
  public String name() {
    return NAME;
  }

  @Override
  public PolicyDecision canBorrow(ReaderLendingContext context) {
    if (context.hasOverdueLending()) {
      return PolicyDecision.denied("Reader has book(s) past their due date");
    }
    if (context.getActiveLendings() >= maxActiveLendings) {
      return PolicyDecision.denied(
          "Reader has " + maxActiveLendings + " books outstanding already");
    }
    return PolicyDecision.allowed();
  }

  @Override
  public LocalDate limitDateFor(LocalDate startDate, ReaderLendingContext context) {
    return startDate.plusDays(lendingDurationInDays);
  }

  @Override
  public PolicyDecision canRenew(RenewalContext context) {
    if (context.isOverdue()) {
      return PolicyDecision.denied("Cannot renew an overdue lending");
    }
    if (context.getRenewalsSoFar() >= maxRenewals) {
      return PolicyDecision.denied(
          "Reader has reached the maximum of " + maxRenewals + " renewals");
    }
    return PolicyDecision.allowed();
  }

  @Override
  public LocalDate renewalLimitDateFor(RenewalContext context) {
    return context.getCurrentLimitDate().plusDays(renewalDurationInDays);
  }

  private static int requirePositive(int value, String field) {
    if (value <= 0) {
      throw new IllegalArgumentException(field + " must be positive, was " + value);
    }
    return value;
  }

  private static int requireNonNegative(int value, String field) {
    if (value < 0) {
      throw new IllegalArgumentException(field + " cannot be negative, was " + value);
    }
    return value;
  }
}
