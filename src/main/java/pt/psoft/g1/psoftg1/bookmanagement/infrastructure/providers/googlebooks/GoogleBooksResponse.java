package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.providers.googlebooks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Internal data holders for the Google Books Volumes API response (#3). Package-private to the
 * adapter, since the Google Books shape must not leak out of this package. Only the fields the
 * adapter maps are declared; {@code @JsonIgnoreProperties(ignoreUnknown = true)} tolerates the many
 * fields Google Books returns that the internal model cannot hold (publisher, published date,
 * cover, identifiers, and the rest), including when decoded by a plain {@code WebClient} in tests.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class GoogleBooksResponse {

  private int totalItems;
  private List<Item> items;

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Item {
    private VolumeInfo volumeInfo;
  }

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class VolumeInfo {
    private String title;
    private String description;
    private List<String> authors;
  }
}
