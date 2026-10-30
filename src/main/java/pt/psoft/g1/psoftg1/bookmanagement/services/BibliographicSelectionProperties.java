package pt.psoft.g1.psoftg1.bookmanagement.services;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized bibliographic source selection (#5, ADR-0012), bound from {@code
 * bibliographic.sources}. The value is an ordered list of source names (for example {@code
 * google-books,open-library}). One name is a single-source setup, several names enable several
 * sources at once in the listed order, which the selecting provider uses for precedence. When the
 * list is empty, the selecting provider defaults to all registered providers. Names are validated
 * against the available providers when the selecting provider is built, so an unknown name fails
 * startup rather than being ignored. This is the setup-time source selection, distinct from the
 * runtime lending-policy selection.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "bibliographic")
public class BibliographicSelectionProperties {

  /** Ordered names of the active bibliographic sources. Empty means all registered providers. */
  private List<String> sources = new ArrayList<>();
}
