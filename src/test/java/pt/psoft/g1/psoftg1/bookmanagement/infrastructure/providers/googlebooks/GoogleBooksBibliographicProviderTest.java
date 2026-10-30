package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.providers.googlebooks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicData;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicLookupException;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicProvider;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicProviderContract;
import reactor.core.publisher.Mono;

/**
 * Opaque-box tests of the Google Books adapter at SUT = the adapter class, with the external call
 * stubbed at the HTTP boundary (#3, ADR-0010). The adapter's {@link WebClient} is built from a stub
 * {@link ExchangeFunction} returning representative Google Books JSON, so the real response
 * decoding and the real mapping are exercised with no network and no new test dependency. Each case
 * asserts the externally observable outcome: the mapped record, an empty result, or the raised
 * exception.
 */
class GoogleBooksBibliographicProviderTest {

  private static final String ISBN = "9782826012092";

  @Test
  void mapsAFullPayloadOntoTheRecordAndEchoesTheQueriedIsbn() {
    String json =
        """
        {
          "kind": "books#volumes",
          "totalItems": 1,
          "items": [
            {
              "id": "abc",
              "volumeInfo": {
                "title": "O Inspetor Max",
                "authors": ["Ana Maria Magalhaes", "Isabel Alcada"],
                "publisher": "Editorial Caminho",
                "publishedDate": "2008",
                "description": "A detective dog story.",
                "industryIdentifiers": [{"type": "ISBN_13", "identifier": "9782826012092"}]
              }
            }
          ]
        }
        """;

    Optional<BibliographicData> result = provider(HttpStatus.OK, json).findByIsbn(ISBN);

    assertThat(result).isPresent();
    BibliographicData data = result.get();
    assertThat(data.getIsbn()).isEqualTo(ISBN);
    assertThat(data.getTitle()).isEqualTo("O Inspetor Max");
    assertThat(data.getDescription()).isEqualTo("A detective dog story.");
    assertThat(data.getAuthorNames()).containsExactly("Ana Maria Magalhaes", "Isabel Alcada");
    assertThat(data.getSource()).isEqualTo("google-books");
  }

  @Test
  void mapsGracefullyWhenDescriptionAndAuthorsAreAbsent() {
    String json =
        """
        {
          "totalItems": 1,
          "items": [ { "volumeInfo": { "title": "Untitled Edition" } } ]
        }
        """;

    Optional<BibliographicData> result = provider(HttpStatus.OK, json).findByIsbn(ISBN);

    assertThat(result).isPresent();
    assertThat(result.get().getDescription()).isNull();
    assertThat(result.get().getAuthorNames()).isEmpty();
  }

  @Test
  void returnsEmptyWhenTheSourceHasNoMatchingVolume() {
    String json = "{ \"kind\": \"books#volumes\", \"totalItems\": 0 }";

    assertThat(provider(HttpStatus.OK, json).findByIsbn(ISBN)).isEmpty();
  }

  @Test
  void throwsLookupExceptionWhenTheVolumeHasNoTitle() {
    // A volume the neutral record rejects (no title) is an unusable response, not a third outcome.
    String json =
        """
        { "totalItems": 1, "items": [ { "volumeInfo": { "authors": ["Ana"] } } ] }
        """;

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

  @Nested
  class Contract extends BibliographicProviderContract {
    @Override
    protected BibliographicProvider providerThatKnows(String isbn, String title) {
      String json =
          """
          {
            "totalItems": 1,
            "items": [
              {
                "volumeInfo": {
                  "title": "%s",
                  "industryIdentifiers": [{"type": "ISBN_13", "identifier": "%s"}]
                }
              }
            ]
          }
          """
              .formatted(title, isbn);
      return provider(HttpStatus.OK, json);
    }

    @Override
    protected BibliographicProvider providerThatKnowsNothing() {
      return provider(HttpStatus.OK, "{ \"kind\": \"books#volumes\", \"totalItems\": 0 }");
    }

    @Override
    protected BibliographicProvider providerThatFails() {
      return provider(HttpStatus.INTERNAL_SERVER_ERROR, "{}");
    }
  }

  /** An adapter whose WebClient returns the given status and JSON body for any request. */
  private static GoogleBooksBibliographicProvider provider(HttpStatus status, String body) {
    ExchangeFunction stub =
        request ->
            Mono.just(
                ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
    WebClient.Builder builder = WebClient.builder().exchangeFunction(stub);
    GoogleBooksProperties properties = new GoogleBooksProperties();
    properties.setBaseUrl("http://localhost");
    return new GoogleBooksBibliographicProvider(builder, properties);
  }
}
