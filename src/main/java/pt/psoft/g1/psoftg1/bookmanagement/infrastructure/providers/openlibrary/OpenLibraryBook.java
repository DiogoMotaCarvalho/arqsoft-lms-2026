package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.providers.openlibrary;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Internal data holder for a single Open Library Books-API entry (#4). The Books API response is a
 * JSON object keyed by the bibkey ({@code ISBN:<isbn>}), so the adapter decodes a map of these. The
 * shape is deliberately distinct from Google Books: the authors are a list of <em>objects carrying a
 * name</em>, not a list of plain strings, which is the evidence that two different external models
 * converge onto one neutral record. Package-private to the adapter, and {@code
 * @JsonIgnoreProperties(ignoreUnknown = true)} tolerates the fields the internal model cannot hold
 * (publishers, publish date, cover, subjects, and the rest), including a plain-WebClient decode in
 * tests.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
class OpenLibraryBook {

  private String title;
  private String description;
  private List<Author> authors;

  @Getter
  @Setter
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Author {
    private String name;
  }
}
