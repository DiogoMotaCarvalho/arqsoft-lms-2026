package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.providers.openlibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicData;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicLookupException;
import reactor.core.publisher.Mono;

/**
 * Opaque-box tests of the Open Library adapter at SUT = the adapter class, with the external call
 * stubbed at the HTTP boundary (#4, ADR-0011). The adapter's {@link WebClient} is built from a stub
 * {@link ExchangeFunction} returning representative Open Library Books-API JSON, so the real
 * response decoding (a map keyed by the bibkey, with authors as objects carrying a name) and the
 * real mapping are exercised with no network and no new test dependency. Each case asserts the
 * externally observable outcome: the mapped record, an empty result, or the raised exception.
 */
class OpenLibraryBibliographicProviderTest {

  private static final String ISBN = "9782826012092";
  private static final String BIBKEY = "ISBN:9782826012092";

  @Test
  void mapsAFullPayloadOntoTheRecordAndEchoesTheQueriedIsbn() {
    String json =
        """
        {
          "%s": {
            "title": "O Inspetor Max",
            "authors": [ {"name": "Ana Maria Magalhaes", "url": "https://openlibrary.org/a/OL1A"},
                         {"name": "Isabel Alcada"} ],
            "publishers": [ {"name": "Editorial Caminho"} ],
            "publish_date": "2008"
          }
        }
        """
            .formatted(BIBKEY);

    Optional<BibliographicData> result = provider(HttpStatus.OK, json).findByIsbn(ISBN);

    assertThat(result).isPresent();
    BibliographicData data = result.get();
    assertThat(data.getIsbn()).isEqualTo(ISBN);
    assertThat(data.getTitle()).isEqualTo("O Inspetor Max");
    assertThat(data.getAuthorNames()).containsExactly("Ana Maria Magalhaes", "Isabel Alcada");
    assertThat(data.getDescription()).isNull(); // the Books API data view carries no description
    assertThat(data.getSource()).isEqualTo("open-library");
  }

  @Test
  void mapsTheDescriptionWhenTheEntryCarriesOne() {
    String json =
        """
        {
          "%s": {
            "title": "O Inspetor Max",
            "authors": [ {"name": "Ana"} ],
            "description": "A detective dog story."
          }
        }
        """
            .formatted(BIBKEY);

    assertThat(provider(HttpStatus.OK, json).findByIsbn(ISBN))
        .get()
        .extracting(BibliographicData::getDescription)
        .isEqualTo("A detective dog story.");
  }

  @Test
  void mapsGracefullyWhenTheEntryHasNoAuthors() {
    String json = "{ \"%s\": { \"title\": \"O Inspetor Max\" } }".formatted(BIBKEY);

    assertThat(provider(HttpStatus.OK, json).findByIsbn(ISBN).orElseThrow().getAuthorNames())
        .isEmpty();
  }

  @Test
  void returnsEmptyWhenTheResponseIsAnEmptyObject() {
    assertThat(provider(HttpStatus.OK, "{}").findByIsbn(ISBN)).isEmpty();
  }

  @Test
  void returnsEmptyWhenTheResponseLacksTheQueriedBibkey() {
    String json = "{ \"ISBN:0000000000000\": { \"title\": \"Other\" } }";

    assertThat(provider(HttpStatus.OK, json).findByIsbn(ISBN)).isEmpty();
  }

  @Test
  void throwsLookupExceptionWhenTheEntryHasNoTitle() {
    String json = "{ \"%s\": { \"authors\": [ {\"name\": \"Ana\"} ] } }".formatted(BIBKEY);

    assertThatExceptionOfType(BibliographicLookupException.class)
        .isThrownBy(() -> provider(HttpStatus.OK, json).findByIsbn(ISBN))
        .withMessageContaining(ISBN);
  }

  @Test
  void throwsLookupExceptionOnANonSuccessStatus() {
    assertThatExceptionOfType(BibliographicLookupException.class)
        .isThrownBy(() -> provider(HttpStatus.INTERNAL_SERVER_ERROR, "{}").findByIsbn(ISBN))
        .withMessageContaining(ISBN);
  }

  @Test
  void throwsLookupExceptionOnAnUnparseableBody() {
    assertThatExceptionOfType(BibliographicLookupException.class)
        .isThrownBy(() -> provider(HttpStatus.OK, "this is not json {").findByIsbn(ISBN))
        .withMessageContaining(ISBN);
  }

  /** An adapter whose WebClient returns the given status and JSON body for any request. */
  private static OpenLibraryBibliographicProvider provider(HttpStatus status, String body) {
    ExchangeFunction stub =
        request ->
            Mono.just(
                ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
    WebClient.Builder builder = WebClient.builder().exchangeFunction(stub);
    OpenLibraryProperties properties = new OpenLibraryProperties();
    properties.setBaseUrl("http://localhost");
    return new OpenLibraryBibliographicProvider(builder, properties);
  }
}
