package pt.psoft.g1.psoftg1.bookmanagement.services;

/**
 * Raised by a {@link BibliographicProvider} adapter when a bibliographic source cannot be reached
 * or its response cannot be understood, for example a network error, a timeout, or an unparseable
 * or garbage response (issue #2, ADR-0008). It is distinct from a successful lookup that finds no
 * book, which is reported as an empty result rather than by throwing, so that the caller can tell
 * "the book is genuinely not there" apart from "the source broke".
 *
 * <p>It is unchecked, so the port signature stays clean and callers opt into handling it.
 */
public class BibliographicLookupException extends RuntimeException {

  public BibliographicLookupException(String message) {
    super(message);
  }

  public BibliographicLookupException(String message, Throwable cause) {
    super(message, cause);
  }
}
