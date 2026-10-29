package pt.psoft.g1.psoftg1.bookmanagement.services;

import java.util.Optional;

/**
 * Port the Book flow calls to fetch bibliographic information about a book, decoupled from any
 * concrete external catalogue (issue #2, ADR-0008). Concrete sources (Google Books in #3, Open
 * Library in #4) are adapters that implement this one port and map their own wire format onto the
 * neutral {@link BibliographicData}. Selecting and configuring the sources (#5) and wiring the lookup
 * into book creation (#6) build on top of this port.
 *
 * <p>The contract fixes two distinct outcomes so that every adapter behaves the same way:
 *
 * <ul>
 *   <li><b>Not found</b>: no book matches the ISBN at the source, reported as {@link
 *       Optional#empty()} (an ordinary result, not an error);
 *   <li><b>Source failure</b>: a network error, a timeout, or an unparseable or garbage response,
 *       reported by throwing {@link BibliographicLookupException}.
 * </ul>
 *
 * <p>No retry or fallback across sources is part of this port. Deciding to try another source on
 * failure is the concern of source selection (#5).
 */
public interface BibliographicProvider {

  /**
   * Looks a book up by its ISBN.
   *
   * @param isbn the ISBN to look up, as a raw string. Validation of the ISBN shape stays the Book
   *     domain's responsibility, not the provider's.
   * @return the bibliographic data if the source has a matching book, or {@link Optional#empty()} if
   *     it does not.
   * @throws BibliographicLookupException if the source cannot be reached or its response cannot be
   *     understood.
   */
  Optional<BibliographicData> findByIsbn(String isbn);
}
