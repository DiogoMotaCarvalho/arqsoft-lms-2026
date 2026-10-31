package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.providers.openlibrary;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicData;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicLookupException;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicProvider;

/**
 * Open Library implementation of the {@link BibliographicProvider} port (#4, ADR-0011). It queries
 * the Open Library Books API by ISBN and maps the matching entry onto the neutral {@link
 * BibliographicData}, so the Open Library shape never leaves this package. It uses the Books API
 * ({@code jscmd=data}) because that endpoint returns author names directly in one call, rather than
 * the edition endpoint whose authors are key references needing a second call each.
 *
 * <p>It honours the port's two-outcome contract: a response with no matching entry is {@link
 * Optional#empty()}, and a transport, decode, or mapping failure (non-2xx status, timeout,
 * unparseable body, or an entry the neutral record rejects) is a {@link
 * BibliographicLookupException}. One blocking call with a configured timeout, no retry or fallback
 * (that is selection, #5).
 *
 * <p>It is a plain {@code @Component}. It now coexists with the Google Books adapter as a second
 * bean of the port type, and choosing between them is the job of selection (#5) and wiring (#6).
 */
@Component
public class OpenLibraryBibliographicProvider implements BibliographicProvider {

  /** Stable source tag recorded on every record this adapter produces. */
  static final String SOURCE = "open-library";

  private static final ParameterizedTypeReference<Map<String, OpenLibraryBook>> RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {};

  private final WebClient webClient;
  private final Duration timeout;

  public OpenLibraryBibliographicProvider(
      WebClient.Builder webClientBuilder, OpenLibraryProperties properties) {
    this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
    this.timeout = properties.getTimeout();
  }

  @Override
  public String source() {
    return SOURCE;
  }

  @Override
  public Optional<BibliographicData> findByIsbn(String isbn) {
    final String bibkey = "ISBN:" + isbn;
    // The whole call and mapping run inside the try, so the port's two-outcome contract holds: a
    // response with no matching entry returns empty, and any transport, decode, or mapping failure
    // surfaces as a BibliographicLookupException rather than a third kind of outcome.
    try {
      final Map<String, OpenLibraryBook> response =
          webClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/api/books")
                          .queryParam("bibkeys", bibkey)
                          .queryParam("format", "json")
                          .queryParam("jscmd", "data")
                          .build())
              .retrieve()
              .bodyToMono(RESPONSE_TYPE)
              .timeout(timeout)
              .block();

      if (response == null) {
        return Optional.empty();
      }
      final OpenLibraryBook book = response.get(bibkey);
      if (book == null) {
        return Optional.empty();
      }
      // The queried ISBN is echoed back, so the record's identity equals what was asked for.
      return Optional.of(
          new BibliographicData(
              isbn, book.getTitle(), book.getDescription(), authorNames(book), SOURCE));
    } catch (RuntimeException e) {
      throw new BibliographicLookupException("Open Library lookup failed for ISBN " + isbn, e);
    }
  }

  /**
   * The author names, or {@code null} when the entry carries none (the record then holds empty).
   */
  private static List<String> authorNames(OpenLibraryBook book) {
    if (book.getAuthors() == null) {
      return null;
    }
    return book.getAuthors().stream()
        .map(OpenLibraryBook.Author::getName)
        .filter(Objects::nonNull)
        .toList();
  }
}
