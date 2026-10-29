package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.providers.openlibrary;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the Open Library adapter (#4, ADR-0011), bound from the {@code
 * bibliographic.open-library} prefix. The base URL has a sensible default and the timeout bounds the
 * single blocking call. There is deliberately no API key, since Open Library requires none, which is
 * a visible contrast with the Google Books adapter's optional key. Nothing is hard-coded in the
 * adapter, which is the C2.1 evidence that a change needs only configuration.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bibliographic.open-library")
public class OpenLibraryProperties {

  /** Base URL of the Open Library API. */
  private String baseUrl = "https://openlibrary.org";

  /** Timeout for the single lookup call. */
  private Duration timeout = Duration.ofSeconds(5);
}
