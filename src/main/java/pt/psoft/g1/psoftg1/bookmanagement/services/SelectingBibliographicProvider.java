package pt.psoft.g1.psoftg1.bookmanagement.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * The configured bibliographic source selection (#5, ADR-0012). It is itself a {@link
 * BibliographicProvider} and the {@link Primary} bean, so the Book flow (#6) injects this one provider
 * and never sees the individual sources. It indexes all provider beans by their {@link
 * BibliographicProvider#source()} name and keeps the configured active ones in order (defaulting to
 * all registered providers when {@code bibliographic.sources} is empty). A configured name with no
 * matching provider fails fast at construction, so a typo fails startup rather than silently yielding
 * nothing.
 *
 * <p>A lookup tries the active sources in the configured order with first-present-wins: a present
 * result is returned at once (later sources are not consulted), a not-found moves to the next source,
 * and a {@link BibliographicLookupException} is recorded and also moves to the next (fallback). After
 * all active sources, if none returned a result and at least one failed, the failure is raised, and
 * otherwise the result is empty. With a single active source this is exactly that source's behaviour,
 * so the port's two-outcome contract is preserved.
 */
@Service
@Primary
public class SelectingBibliographicProvider implements BibliographicProvider {

  /** Stable source name of the selecting provider itself (not a concrete catalogue). */
  static final String SOURCE = "selecting";

  private final List<BibliographicProvider> activeInOrder;

  public SelectingBibliographicProvider(
      List<BibliographicProvider> providers, BibliographicSelectionProperties properties) {
    // Spring excludes the bean under construction from a collection it injects, so the selecting
    // provider never appears among its own providers.
    final Map<String, BibliographicProvider> byName = new LinkedHashMap<>();
    for (BibliographicProvider provider : providers) {
      byName.put(provider.source(), provider);
    }

    final List<String> configured = properties.getSources();
    if (configured == null || configured.isEmpty()) {
      this.activeInOrder = new ArrayList<>(byName.values()); // default: all, in discovery order
      return;
    }

    final List<BibliographicProvider> active = new ArrayList<>();
    for (String name : configured) {
      final BibliographicProvider provider = byName.get(name);
      if (provider == null) {
        throw new IllegalArgumentException(
            "Unknown bibliographic source configured: '"
                + name
                + "'. Available sources: "
                + byName.keySet());
      }
      active.add(provider);
    }
    this.activeInOrder = active;
  }

  @Override
  public String source() {
    return SOURCE;
  }

  @Override
  public Optional<BibliographicData> findByIsbn(String isbn) {
    BibliographicLookupException lastFailure = null;
    for (BibliographicProvider provider : activeInOrder) {
      try {
        final Optional<BibliographicData> result = provider.findByIsbn(isbn);
        if (result.isPresent()) {
          return result;
        }
      } catch (BibliographicLookupException e) {
        lastFailure = e; // fall back to the next active source, keeping the cause
      }
    }
    if (lastFailure != null) {
      throw new BibliographicLookupException(
          "All active bibliographic sources failed for ISBN " + isbn, lastFailure);
    }
    return Optional.empty();
  }
}
