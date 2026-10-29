package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.providers.googlebooks;

import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicData;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicLookupException;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicProvider;

/**
 * Google Books implementation of the {@link BibliographicProvider} port (#3, ADR-0010). It queries
 * the Google Books Volumes API by ISBN and maps the first returned volume onto the neutral {@link
 * BibliographicData}, so the Google Books shape never leaves this package.
 *
 * <p>It honours the port's two-outcome contract: a response with no matching volume is {@link
 * Optional#empty()}, and a transport or decode failure (non-2xx status, timeout, unparseable body)
 * is a {@link BibliographicLookupException}. It makes a single blocking call with a configured
 * timeout and performs no retry or fallback, which is the concern of selection (#5).
 *
 * <p>It is a plain {@code @Component} with no wiring and no qualifier. When the Open Library adapter
 * (#4) is added there will be two beans of the port type, and choosing between them is the job of
 * selection (#5) and of wiring the lookup into the Book flow (#6).
 */
@Component
public class GoogleBooksBibliographicProvider implements BibliographicProvider {

  /** Stable source tag recorded on every record this adapter produces. */
  static final String SOURCE = "google-books";

  private final WebClient webClient;
  private final String apiKey;
  private final Duration timeout;

  public GoogleBooksBibliographicProvider(
      WebClient.Builder webClientBuilder, GoogleBooksProperties properties) {
    this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
    this.apiKey = properties.getApiKey();
    this.timeout = properties.getTimeout();
  }

  @Override
  public String source() {
    return SOURCE;
  }

  @Override
  public Optional<BibliographicData> findByIsbn(String isbn) {
    // The whole call and mapping run inside the try, so the port's two-outcome contract holds: a
    // response with no usable volume returns empty, and any transport, decode, or mapping failure
    // (including a volume the neutral record rejects, such as one with no title) surfaces as a
    // BibliographicLookupException rather than leaking a third kind of outcome.
    try {
      final GoogleBooksResponse response =
          webClient
              .get()
              .uri(
                  uriBuilder -> {
                    uriBuilder.path("/volumes").queryParam("q", "isbn:" + isbn);
                    if (apiKey != null && !apiKey.isBlank()) {
                      uriBuilder.queryParam("key", apiKey);
                    }
                    return uriBuilder.build();
                  })
              .retrieve()
              .bodyToMono(GoogleBooksResponse.class)
              .timeout(timeout)
              .block();

      if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
        return Optional.empty();
      }
      final GoogleBooksResponse.VolumeInfo info = response.getItems().get(0).getVolumeInfo();
      if (info == null) {
        return Optional.empty();
      }
      // The queried ISBN is echoed back, so the record's identity equals what was asked for.
      return Optional.of(
          new BibliographicData(
              isbn, info.getTitle(), info.getDescription(), info.getAuthors(), SOURCE));
    } catch (RuntimeException e) {
      throw new BibliographicLookupException("Google Books lookup failed for ISBN " + isbn, e);
    }
  }
}
