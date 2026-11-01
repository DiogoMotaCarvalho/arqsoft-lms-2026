package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable value object carrying the outcome of a {@link LendingPolicy} eligibility check.
 *
 * <p>Policies report denial by returning a {@code PolicyDecision} rather than throwing, so that the
 * selection logic (#12) can compose and compare outcomes and so that policy behaviour is cheap to
 * unit-test. The service layer is responsible for mapping a denial to the appropriate exception.
 */
public final class PolicyDecision {

  private static final PolicyDecision ALLOWED = new PolicyDecision(true, null);

  private final boolean allowed;
  private final String reason;

  private PolicyDecision(boolean allowed, String reason) {
    this.allowed = allowed;
    this.reason = reason;
  }

  /** The decision allows the action; carries no reason. */
  public static PolicyDecision allowed() {
    return ALLOWED;
  }

  /**
   * The decision denies the action.
   *
   * @param reason human-readable justification, never {@code null} or blank.
   */
  public static PolicyDecision denied(String reason) {
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("A denial must carry a non-blank reason");
    }
    return new PolicyDecision(false, reason);
  }

  public boolean isAllowed() {
    return allowed;
  }

  public boolean isDenied() {
    return !allowed;
  }

  /** The denial reason, present only when the decision is a denial. */
  public Optional<String> getReason() {
    return Optional.ofNullable(reason);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PolicyDecision)) {
      return false;
    }
    PolicyDecision that = (PolicyDecision) o;
    return allowed == that.allowed && Objects.equals(reason, that.reason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowed, reason);
  }

  @Override
  public String toString() {
    return allowed ? "PolicyDecision[allowed]" : "PolicyDecision[denied: " + reason + "]";
  }
}
