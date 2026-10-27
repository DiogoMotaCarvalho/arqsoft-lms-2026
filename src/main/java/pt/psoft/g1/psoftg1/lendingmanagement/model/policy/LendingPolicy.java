package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import java.time.LocalDate;

/**
 * A lending policy: the set of rules that govern lending decisions for a reader. The system
 * supports more than one policy (e.g. Standard, Adaptive) and selects which one applies.
 *
 * <p>This is a pure, framework-free domain abstraction (no Spring, no JPA). It is <em>behaviour
 * only</em>: a policy's quantitative parameters are not part of this interface — they are injected
 * into concrete policies at construction and validated there (fail-fast). A policy owns exactly
 * three decisions:
 *
 * <ol>
 *   <li>borrow-eligibility for a new lending (max active lendings + no borrowing while overdue);
 *   <li>the lending period / limit date of a new lending;
 *   <li>renewal eligibility and the renewed limit date.
 * </ol>
 *
 * <p>Eligibility decisions are reported as a {@link PolicyDecision} (allowed + reason); policies
 * never throw to signal denial. Fine calculation is explicitly out of scope and stays on the {@code
 * Lending} entity.
 */
public interface LendingPolicy {

  /**
   * The policy's stable identifier, used for configuration and selection (e.g. {@code "standard"},
   * {@code "adaptive"}).
   */
  String name();

  /** Whether the reader may start a new lending, given their current lending situation. */
  PolicyDecision canBorrow(ReaderLendingContext context);

  /**
   * The limit date of a new lending starting on {@code startDate}. May depend on the reader's
   * situation (e.g. the Adaptive policy shortens the period as active lendings grow).
   */
  LocalDate limitDateFor(LocalDate startDate, ReaderLendingContext context);

  /** Whether an existing lending may be renewed, given its renewal situation. */
  PolicyDecision canRenew(RenewalContext context);

  /** The limit date an existing lending would have after a renewal. */
  LocalDate renewalLimitDateFor(RenewalContext context);
}
