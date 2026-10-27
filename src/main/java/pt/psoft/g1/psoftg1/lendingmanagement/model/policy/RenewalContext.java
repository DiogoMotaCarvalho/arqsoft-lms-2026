package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The facts a {@link LendingPolicy} needs to decide a <em>renewal</em> of an existing lending: its
 * current limit date, how many times it has already been renewed, and whether it is currently
 * overdue.
 *
 * <p>The renewal endpoint/service wiring is deferred to #13; only the policy methods that consume
 * this context exist in this ticket.
 */
public final class RenewalContext {

  private final LocalDate currentLimitDate;
  private final int renewalsSoFar;
  private final boolean overdue;

  public RenewalContext(LocalDate currentLimitDate, int renewalsSoFar, boolean overdue) {
    this.currentLimitDate =
        Objects.requireNonNull(currentLimitDate, "currentLimitDate is required");
    if (renewalsSoFar < 0) {
      throw new IllegalArgumentException("renewalsSoFar cannot be negative");
    }
    this.renewalsSoFar = renewalsSoFar;
    this.overdue = overdue;
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
}
