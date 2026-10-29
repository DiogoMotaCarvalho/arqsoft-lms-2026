package pt.psoft.g1.psoftg1.bookmanagement.services;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Supplies the external bibliographic data used to enrich a retrieved book (#6, ADR-0013). It owns the
 * external call, the on/off switch, and the failure handling, so the resilience rules live in one
 * place. It is gated by {@code bibliographic.enrichment.enabled} (default true): when off it returns
 * nothing and makes no external call. It delegates to the configured primary {@link
 * BibliographicProvider} (the selecting provider of #5) and is best-effort, so a {@link
 * BibliographicLookupException} is swallowed and reported as an empty result, since a retrieval must
 * never fail because an external source is down. The gap-merge into the view stays in the presentation
 * layer, so this service does not depend on a view type.
 */
@Service
public class BibliographicEnrichmentService {

  private final BibliographicProvider provider;
  private final boolean enabled;

  public BibliographicEnrichmentService(
      BibliographicProvider provider,
      @Value("${bibliographic.enrichment.enabled:true}") boolean enabled) {
    this.provider = provider;
    this.enabled = enabled;
  }

  /**
   * The external bibliographic data for the given ISBN, or empty when enrichment is disabled, when no
   * source has the book, or when every active source is unreachable.
   */
  public Optional<BibliographicData> enrichmentFor(String isbn) {
    if (!enabled) {
      return Optional.empty();
    }
    try {
      return provider.findByIsbn(isbn);
    } catch (BibliographicLookupException e) {
      return Optional.empty(); // best-effort: a source outage must not break retrieval
    }
  }
}
