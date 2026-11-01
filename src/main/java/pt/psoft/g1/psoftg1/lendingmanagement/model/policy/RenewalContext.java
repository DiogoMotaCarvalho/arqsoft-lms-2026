package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The facts a {@link LendingPolicy} needs to decide a <em>renewal</em> of an existing lending: its
 * current limit date, how many times it has already been renewed, whether it is currently overdue,
 * and how many active lendings the reader holds.
 *
 * <p>{@code activeLendings} lets a situation-dependent policy (the Adaptive policy) pick the
 * renewed lending period for the reader's current band; the Standard policy ignores it. The renewal
 * endpoint/service wiring is deferred to #13; only the policy methods that consume this context
 * exist today, and the renewal flow will supply {@code activeLendings} then.
 */
public final class RenewalContext {

  private final LocalDate currentLimitDate;
  private final int renewalsSoFar;
  private final boolean overdue;
  private final int activeLendings;

  /** For policies that do not depend on the reader's active-lending count (e.g. Standard). */
  public RenewalContext(LocalDate currentLimitDate, int renewalsSoFar, boolean overdue) {
    this(currentLimitDate, renewalsSoFar, overdue, 0);
  }

  public RenewalContext(
      LocalDate currentLimitDate, int renewalsSoFar, boolean overdue, int activeLendings) {
    this.currentLimitDate =
        Objects.requireNonNull(currentLimitDate, "currentLimitDate is required");
    if (renewalsSoFar < 0) {
      throw new IllegalArgumentException("renewalsSoFar cannot be negative");
    }
    if (activeLendings < 0) {
      throw new IllegalArgumentException("activeLendings cannot be negative");
    }
    this.renewalsSoFar = renewalsSoFar;
    this.overdue = overdue;
    this.activeLendings = activeLendings;
  }

  public LocalDate getCurrentLimitDate() {
    return currentLimitDate;
  }

  public int getRenewalsSoFar() {
    return renewalsSoFar;
  }

  public boolean isOverdue() {
    return overdue;
  }

  public int getActiveLendings() {
    return activeLendings;
  }
}
