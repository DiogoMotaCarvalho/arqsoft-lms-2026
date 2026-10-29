package pt.psoft.g1.psoftg1.bookmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests the configured source selection (#5, ADR-0012) with in-memory stub providers, asserting the
 * observable outcome of a lookup across the active sources rather than how iteration works. Covers the
 * single-source and multi-source cases, the order and fallback behaviour, and the fail-fast rejection
 * of an unknown configured source.
 */
class SelectingBibliographicProviderTest {

  private static final String ISBN = "9782826012092";

  // --- single source ---

  @Test
  void singleSourceWithTheBookReturnsIt() {
    SelectingBibliographicProvider selector =
        selector(List.of(present("a")), props("a"));

    assertThat(selector.findByIsbn(ISBN)).get().extracting(BibliographicData::getSource).isEqualTo("a");
  }

  @Test
  void singleSourceWithoutTheBookReturnsEmpty() {
    SelectingBibliographicProvider selector = selector(List.of(empty("a")), props("a"));

    assertThat(selector.findByIsbn(ISBN)).isEmpty();
  }

  @Test
  void singleSourceThatFailsRaisesTheLookupException() {
    SelectingBibliographicProvider selector = selector(List.of(failing("a")), props("a"));

    assertThatExceptionOfType(BibliographicLookupException.class)
        .isThrownBy(() -> selector.findByIsbn(ISBN));
  }

  // --- multi source ---

  @Test
  void firstPresentWinsAndLaterSourcesAreNotConsulted() {
    SelectingBibliographicProvider selector =
        selector(List.of(present("a"), neverConsulted("z")), props("a", "z"));

    assertThat(selector.findByIsbn(ISBN)).get().extracting(BibliographicData::getSource).isEqualTo("a");
  }

  @Test
  void aNotFoundSourceFallsThroughToTheNext() {
    SelectingBibliographicProvider selector =
        selector(List.of(empty("a"), present("b")), props("a", "b"));

    assertThat(selector.findByIsbn(ISBN)).get().extracting(BibliographicData::getSource).isEqualTo("b");
  }

  @Test
  void aFailingSourceFallsThroughToTheNext() {
    SelectingBibliographicProvider selector =
        selector(List.of(failing("a"), present("b")), props("a", "b"));

    assertThat(selector.findByIsbn(ISBN)).get().extracting(BibliographicData::getSource).isEqualTo("b");
  }

  @Test
  void allSourcesNotFoundReturnsEmpty() {
    SelectingBibliographicProvider selector =
        selector(List.of(empty("a"), empty("b")), props("a", "b"));

    assertThat(selector.findByIsbn(ISBN)).isEmpty();
  }

  @Test
  void everySourceFailingRaisesTheLookupException() {
    SelectingBibliographicProvider selector =
        selector(List.of(failing("a"), failing("b")), props("a", "b"));

    assertThatExceptionOfType(BibliographicLookupException.class)
        .isThrownBy(() -> selector.findByIsbn(ISBN));
  }

  @Test
  void theConfiguredOrderDeterminesPrecedence() {
    List<BibliographicProvider> both = List.of(present("a"), present("b"));

    assertThat(selector(both, props("b", "a")).findByIsbn(ISBN))
        .get()
        .extracting(BibliographicData::getSource)
        .isEqualTo("b");
    assertThat(selector(both, props("a", "b")).findByIsbn(ISBN))
        .get()
        .extracting(BibliographicData::getSource)
        .isEqualTo("a");
  }

  // --- configuration ---

  @Test
  void anUnknownConfiguredSourceIsRejectedAtConstruction() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> selector(List.of(present("a")), props("does-not-exist")))
        .withMessageContaining("does-not-exist");
  }

  @Test
  void anEmptyConfigurationDefaultsToAllRegisteredSources() {
    // no configured list: both are active, so the one that has the book answers
    SelectingBibliographicProvider selector = selector(List.of(empty("a"), present("b")), props());

    assertThat(selector.findByIsbn(ISBN)).get().extracting(BibliographicData::getSource).isEqualTo("b");
  }

  // --- helpers ---

  private static SelectingBibliographicProvider selector(
      List<BibliographicProvider> providers, BibliographicSelectionProperties properties) {
    return new SelectingBibliographicProvider(providers, properties);
  }

  private static BibliographicSelectionProperties props(String... sources) {
    BibliographicSelectionProperties p = new BibliographicSelectionProperties();
    p.setSources(List.of(sources));
    return p;
  }

  private static BibliographicData data(String source) {
    return new BibliographicData(ISBN, "Title from " + source, null, List.of(), source);
  }

  /** A source that has the book, tagged with its source name. */
  private static BibliographicProvider present(String source) {
    return new InMemoryBibliographicProvider().named(source).with(data(source));
  }

  /** A reachable source that has no matching book. */
  private static BibliographicProvider empty(String source) {
    return new InMemoryBibliographicProvider().named(source);
  }

  /** A source that is down (throws the lookup exception). */
  private static BibliographicProvider failing(String source) {
    return new InMemoryBibliographicProvider().named(source).failingFor(ISBN, "down");
  }

  /** A source that must not be consulted: throws if it is. */
  private static BibliographicProvider neverConsulted(String source) {
    return new BibliographicProvider() {
      @Override
      public String source() {
        return source;
      }

      @Override
      public Optional<BibliographicData> findByIsbn(String isbn) {
        throw new AssertionError("source '" + source + "' should not have been consulted");
      }
    };
  }
}
