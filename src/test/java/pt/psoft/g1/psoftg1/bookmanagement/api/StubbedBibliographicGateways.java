package pt.psoft.g1.psoftg1.bookmanagement.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Test support for the bibliographic enrichment tests (#6, ADR-0013). It stubs the external HTTP at
 * the boundary while keeping the real adapters and the real selecting provider: it supplies a
 * {@link WebClient.Builder} whose stub {@link ExchangeFunction} routes by request host (Google
 * Books versus Open Library) and by the queried ISBN, returning representative payloads of the two
 * distinct source models. The real adapters build their clients from this builder, so the whole
 * stack runs and only the network is stubbed.
 */
@TestConfiguration
public class StubbedBibliographicGateways {

  /** A book both sources have; Google Books is first, so its description wins. */
  public static final String ISBN_ENRICHED = "9781603090001";

  /** A book Google Books does not have but Open Library does (multi-source fallback). */
  public static final String ISBN_FALLBACK = "9781603090018";

  /** A book for which every source fails (a total outage). */
  public static final String ISBN_FAIL = "9781603090025";

  /** A book no source has (not-found). */
  public static final String ISBN_NO_MATCH = "9781603090032";

  /** A book whose local description is present, so enrichment must not overwrite it. */
  public static final String ISBN_LOCAL_KEPT = "9781603090049";

  @Bean
  @Scope("prototype")
  WebClient.Builder webClientBuilder() {
    return WebClient.builder().exchangeFunction(stub());
  }

  private static ExchangeFunction stub() {
    return request -> {
      final String url = request.url().toString();
      final boolean google = request.url().getHost().contains("googleapis");

      HttpStatus status = HttpStatus.OK;
      String body;
      if (url.contains(ISBN_FAIL)) {
        status = HttpStatus.INTERNAL_SERVER_ERROR;
        body = "{}";
      } else if (url.contains(ISBN_ENRICHED)) {
        body =
            google
                ? google("Enriched by Google Books")
                : openLibrary(ISBN_ENRICHED, "Enriched by Open Library");
      } else if (url.contains(ISBN_FALLBACK)) {
        body = google ? googleEmpty() : openLibrary(ISBN_FALLBACK, "Enriched by Open Library");
      } else if (url.contains(ISBN_LOCAL_KEPT)) {
        body = google ? google("External description that must be ignored") : googleEmpty();
      } else {
        body = google ? googleEmpty() : openLibraryEmpty();
      }

      return Mono.just(
          ClientResponse.create(status)
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
              .body(body)
              .build());
    };
  }

  private static String google(String description) {
    return "{\"totalItems\":1,\"items\":[{\"volumeInfo\":{\"title\":\"T\",\"description\":\""
        + description
        + "\"}}]}";
  }

  private static String googleEmpty() {
    return "{\"totalItems\":0}";
  }

  private static String openLibrary(String isbn, String description) {
    return "{\"ISBN:"
        + isbn
        + "\":{\"title\":\"T\",\"description\":\""
        + description
        + "\",\"authors\":[{\"name\":\"A\"}]}}";
  }

  private static String openLibraryEmpty() {
    return "{}";
  }
}
