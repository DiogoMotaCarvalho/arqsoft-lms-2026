package pt.psoft.g1.psoftg1.bookmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link BibliographicProvider} contract against an in-memory stub, with no network and
 * no Spring context (issue #2, ADR-0008). It proves the two outcomes every adapter must honour: a
 * known ISBN yields a populated result, an unknown ISBN yields an empty result (not an error), and
 * a source failure is reported by throwing {@link BibliographicLookupException}.
 */
class BibliographicProviderContractTest {

  private static final String KNOWN_ISBN = "9782826012092";

  @Test
  void returnsThePopulatedRecordForAKnownIsbn() {
    BibliographicProvider provider =
        new InMemoryBibliographicProvider()
            .with(
                new BibliographicData(
                    KNOWN_ISBN, "O Inspetor Max", "A description", List.of("Ana"), "stub"));

    Optional<BibliographicData> result = provider.findByIsbn(KNOWN_ISBN);

    assertThat(result).isPresent();
    assertThat(result.get().getTitle()).isEqualTo("O Inspetor Max");
    assertThat(result.get().getSource()).isEqualTo("stub");
  }

  @Test
  void returnsEmptyForAnUnknownIsbn() {
    BibliographicProvider provider = new InMemoryBibliographicProvider();

    assertThat(provider.findByIsbn("0000000000000")).isEmpty();
  }

  @Test
  void throwsOnSourceFailure() {
    BibliographicProvider provider =
        new InMemoryBibliographicProvider().failingFor(KNOWN_ISBN, "source unreachable");

    assertThatExceptionOfType(BibliographicLookupException.class)
        .isThrownBy(() -> provider.findByIsbn(KNOWN_ISBN))
        .withMessageContaining("unreachable");
  }
}
