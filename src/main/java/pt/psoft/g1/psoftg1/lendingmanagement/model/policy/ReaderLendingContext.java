package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

/**
 * The reader-side facts a {@link LendingPolicy} needs to decide a <em>new</em> lending: how many
 * active lendings the reader currently holds and whether any of them is overdue.
 *
 * <p>"Active lending" follows the glossary: a lending that has been started and not yet returned
 * (an overdue lending is still active). This context is plain data so policies stay framework-free
 * and directly unit-testable; the service builds it from the reader's outstanding lendings.
 */
public final class ReaderLendingContext {

  private final int activeLendings;
  private final boolean hasOverdueLending;

  public ReaderLendingContext(int activeLendings, boolean hasOverdueLending) {
    if (activeLendings < 0) {
      throw new IllegalArgumentException("activeLendings cannot be negative");
    }
    this.activeLendings = activeLendings;
    this.hasOverdueLending = hasOverdueLending;
  }

  public int getActiveLendings() {
    return activeLendings;
  }

  public boolean hasOverdueLending() {
    return hasOverdueLending;
  }
}
