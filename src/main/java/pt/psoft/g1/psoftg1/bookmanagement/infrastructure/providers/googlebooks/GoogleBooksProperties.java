package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.providers.googlebooks;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the Google Books adapter (#3, ADR-0010), bound from the {@code
 * bibliographic.google-books} prefix. The base URL has a sensible default, the API key is optional
 * (Google Books volume lookups work without one, subject to rate limits), and the timeout bounds
 * the single blocking call. None of these is hard-coded in the adapter, which is the C2.1 evidence
 * that a change needs only configuration, no code edit.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bibliographic.google-books")
public class GoogleBooksProperties {

  /** Base URL of the Google Books API. */
  private String baseUrl = "https://www.googleapis.com/books/v1";

  /** Optional API key. When blank or absent, the adapter calls the API without a key. */
  private String apiKey;

  /** Timeout for the single lookup call. */
  private Duration timeout = Duration.ofSeconds(5);
}
