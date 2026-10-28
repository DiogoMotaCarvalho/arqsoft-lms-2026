package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.time.LocalDate;

/**
 * The Adaptive lending policy: the lending period shortens as the reader's active-lending count
 * rises across two thresholds. This is a {@link LendingPolicy} seam implementation (ADR-0001); its
 * parameters come from the externalized, validated configuration (#9) and its interpretation of the
 * under-specified renewal rules is recorded in ADR-0004.
 *
 * <p>Rules (lending spec §3):
 *
 * <ul>
 *   <li>a reader may not borrow while any active lending is overdue;
 *   <li>a reader may not hold more than {@code maxActiveLendings} active lendings at once;
 *   <li>the lending period is chosen from the reader's <em>current</em> active-lending count (the
 *       new lending not yet counted): {@code < firstThreshold} → {@code longPeriod}; {@code
 *       [firstThreshold, secondThreshold)} → {@code standardPeriod}; {@code ≥ secondThreshold} →
 *       {@code shortPeriod};
 *   <li>a lending may be renewed while it is not overdue (there is no renewal cap for this policy);
 *       the renewed period is taken from the reader's current band.
 * </ul>
 *
 * <p>All parameters are validated at construction (fail-fast), including the cross-field ordering
 * {@code 0 < firstThreshold < secondThreshold < maxActiveLendings} and {@code longPeriod >
 * standardPeriod > shortPeriod > 0}; an invalid configuration cannot produce a policy instance.
 */
public final class AdaptiveLendingPolicy implements LendingPolicy {

  /** Stable identifier of this policy. */
  public static final String NAME = "adaptive";

  private final int maxActiveLendings;
  private final int firstThreshold;
  private final int secondThreshold;
  private final int longPeriod;
  private final int standardPeriod;
  private final int shortPeriod;

  public AdaptiveLendingPolicy(
      int maxActiveLendings,
      int firstThreshold,
      int secondThreshold,
      int longPeriod,
      int standardPeriod,
      int shortPeriod) {
    requirePositive(maxActiveLendings, "maxActiveLendings");
    requirePositive(firstThreshold, "firstThreshold");
    requirePositive(secondThreshold, "secondThreshold");
    requirePositive(longPeriod, "longPeriod");
    requirePositive(standardPeriod, "standardPeriod");
    requirePositive(shortPeriod, "shortPeriod");
    if (!(firstThreshold < secondThreshold && secondThreshold < maxActiveLendings)) {
      throw new IllegalArgumentException(
          "thresholds must satisfy 0 < firstThreshold < secondThreshold < maxActiveLendings");
    }
    if (!(longPeriod > standardPeriod && standardPeriod > shortPeriod)) {
      throw new IllegalArgumentException(
          "periods must satisfy longPeriod > standardPeriod > shortPeriod > 0");
    }
    this.maxActiveLendings = maxActiveLendings;
    this.firstThreshold = firstThreshold;
    this.secondThreshold = secondThreshold;
    this.longPeriod = longPeriod;
    this.standardPeriod = standardPeriod;
    this.shortPeriod = shortPeriod;
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
    return startDate.plusDays(periodForActiveLendings(context.getActiveLendings()));
  }

  @Override
  public PolicyDecision canRenew(RenewalContext context) {
    if (context.isOverdue()) {
      return PolicyDecision.denied("Cannot renew an overdue lending");
    }
    return PolicyDecision.allowed();
  }

  @Override
  public LocalDate renewalLimitDateFor(RenewalContext context) {
    return context
        .getCurrentLimitDate()
        .plusDays(periodForActiveLendings(context.getActiveLendings()));
  }

  /** The lending period (in days) for a reader holding {@code activeLendings} active lendings. */
  private int periodForActiveLendings(int activeLendings) {
    if (activeLendings < firstThreshold) {
      return longPeriod;
    }
    if (activeLendings < secondThreshold) {
      return standardPeriod;
    }
    return shortPeriod;
  }

  private static void requirePositive(int value, String field) {
    if (value <= 0) {
      throw new IllegalArgumentException(field + " must be positive, was " + value);
    }
  }
}
