package pt.psoft.g1.psoftg1.bookmanagement.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * An in-memory {@link BibliographicProvider} for tests (issue #2). It lets the port contract be
 * exercised with no network and no Spring context: records added to it are found by ISBN, and any
 * other ISBN is not found. A source failure can be simulated by registering an ISBN as failing.
 *
 * <p>This is test support only. The real sources are the Google Books (#3) and Open Library (#4)
 * adapters.
 */
class InMemoryBibliographicProvider implements BibliographicProvider {

  private final Map<String, BibliographicData> byIsbn = new HashMap<>();
  private final Map<String, String> failingIsbns = new HashMap<>();
  private String source = "in-memory";

  /** Sets the stable source name this stub reports from {@link #source()}. */
  InMemoryBibliographicProvider named(String source) {
    this.source = source;
    return this;
  }

  @Override
  public String source() {
    return source;
  }

  InMemoryBibliographicProvider with(BibliographicData data) {
    byIsbn.put(data.getIsbn(), data);
    return this;
  }

  InMemoryBibliographicProvider failingFor(String isbn, String reason) {
    failingIsbns.put(isbn, reason);
    return this;
  }

  @Override
  public Optional<BibliographicData> findByIsbn(String isbn) {
    if (failingIsbns.containsKey(isbn)) {
      throw new BibliographicLookupException(failingIsbns.get(isbn));
    }
    return Optional.ofNullable(byIsbn.get(isbn));
  }
}
