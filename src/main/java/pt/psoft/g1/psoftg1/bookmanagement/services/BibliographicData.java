package pt.psoft.g1.psoftg1.bookmanagement.services;

import java.util.List;

/**
 * The neutral bibliographic record returned by a {@link BibliographicProvider} (issue #2, ADR-0008).
 * It carries only fields the internal {@code Book} model can hold, so that heterogeneous external
 * responses are mapped onto one source-independent shape and nothing a catalogue returns that the
 * domain cannot use (publisher, published date) leaks in.
 *
 * <p>This is a framework-free, immutable value object. Construction is the mapping that this issue
 * owns and that the transparent-box tests exercise. It fails fast only when a required field (isbn or
 * title) is absent or blank. Absent optional fields are handled gracefully: {@code description} may be
 * {@code null}, and {@code authorNames} is never {@code null} (an absent author list becomes empty).
 * The {@code source} tag names the origin of the data (for example {@code "google-books"}), which the
 * selection logic (#5) and debugging use. An adapter is expected to set it, but it is not a required
 * field and is not fail-fast validated here, so it may be {@code null}.
 *
 * <p>The source-specific parsing from a catalogue's wire format onto this record belongs to each
 * adapter (#3, #4), because only the adapter knows its own format.
 */
public final class BibliographicData {

  private final String isbn;
  private final String title;
  private final String description;
  private final List<String> authorNames;
  private final String source;

  public BibliographicData(
      String isbn, String title, String description, List<String> authorNames, String source) {
    this.isbn = requireNonBlank(isbn, "isbn");
    this.title = requireNonBlank(title, "title");
    this.description = description;
    this.authorNames = authorNames == null ? List.of() : List.copyOf(authorNames);
    this.source = source;
  }

  public String getIsbn() {
    return isbn;
  }

  public String getTitle() {
    return title;
  }

  /** The description, or {@code null} when the source did not supply one. */
  public String getDescription() {
    return description;
  }

  /** The author names, never {@code null} and possibly empty. The returned list is unmodifiable. */
  public List<String> getAuthorNames() {
    return authorNames;
  }

  /**
   * The origin of the data, for example the name of the bibliographic source, or {@code null} when
   * the adapter did not set one.
   */
  public String getSource() {
    return source;
  }

  private static String requireNonBlank(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value;
  }
}
