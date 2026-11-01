package pt.psoft.g1.psoftg1.bookmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the enrichment service (#6, ADR-0013) at its own seam: the toggle and the
 * best-effort failure handling. Driven with an in-memory stub provider, asserting the observable
 * outcome (the returned optional) rather than how the provider is called.
 */
class BibliographicEnrichmentServiceTest {

  private static final String ISBN = "9782826012092";

  @Test
  void returnsTheProviderResultWhenEnabled() {
    BibliographicProvider provider =
        new InMemoryBibliographicProvider()
            .named("stub")
            .with(new BibliographicData(ISBN, "A title", "A description", List.of(), "stub"));

    BibliographicEnrichmentService service = new BibliographicEnrichmentService(provider, true);

    assertThat(service.enrichmentFor(ISBN))
        .get()
        .extracting(BibliographicData::getDescription)
        .isEqualTo("A description");
  }

  @Test
  void returnsEmptyAndDoesNotCallTheProviderWhenDisabled() {
    BibliographicProvider provider =
        new BibliographicProvider() {
          @Override
          public String source() {
            return "stub";
          }

          @Override
          public java.util.Optional<BibliographicData> findByIsbn(String isbn) {
            throw new AssertionError("provider must not be consulted when enrichment is disabled");
          }
        };

    BibliographicEnrichmentService service = new BibliographicEnrichmentService(provider, false);

    assertThat(service.enrichmentFor(ISBN)).isEmpty();
  }

  @Test
  void swallowsASourceFailureAndReturnsEmpty() {
    BibliographicProvider provider =
        new InMemoryBibliographicProvider().named("stub").failingFor(ISBN, "down");

    BibliographicEnrichmentService service = new BibliographicEnrichmentService(provider, true);

    assertThat(service.enrichmentFor(ISBN)).isEmpty();
  }

  @Test
  void returnsEmptyWhenEnabledButNoSourceHasTheBook() {
    BibliographicProvider provider = new InMemoryBibliographicProvider().named("stub");

    BibliographicEnrichmentService service = new BibliographicEnrichmentService(provider, true);

    assertThat(service.enrichmentFor(ISBN)).isEmpty();
  }
}
