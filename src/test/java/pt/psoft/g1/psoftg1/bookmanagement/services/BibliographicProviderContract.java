package pt.psoft.g1.psoftg1.bookmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Optional;
import org.junit.jupiter.api.Test;

public abstract class BibliographicProviderContract {
  protected static final String KNOWN_ISBN = "9782826012092";
  protected static final String KNOWN_TITLE = "O Inspetor Max";

  protected abstract BibliographicProvider providerThatKnows(String isbn, String title);

  protected abstract BibliographicProvider providerThatKnowsNothing();

  protected abstract BibliographicProvider providerThatFails();

  @Test
  void returnsTheRecordForAKnownIsbn() {
    Optional<BibliographicData> result =
        providerThatKnows(KNOWN_ISBN, KNOWN_TITLE).findByIsbn(KNOWN_ISBN);

    assertThat(result).isPresent();
    assertThat(result.get().getIsbn()).isEqualTo(KNOWN_ISBN);
    assertThat(result.get().getTitle()).isEqualTo(KNOWN_TITLE);
    assertThat(result.get().getSource()).isNotBlank();
  }

  @Test
  void returnsEmptyForAnUnknownIsbn() {
    assertThat(providerThatKnowsNothing().findByIsbn(KNOWN_ISBN)).isEmpty();
  }

  @Test
  void reportsSourceFailureAsLookupException() {
    BibliographicProvider provider = providerThatFails();

    assertThatExceptionOfType(BibliographicLookupException.class)
        .isThrownBy(() -> provider.findByIsbn(KNOWN_ISBN));
  }
}
