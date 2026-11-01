package pt.psoft.g1.psoftg1.bookmanagement.services;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link BibliographicSelectionProperties} so {@code bibliographic.sources} binds. */
@Configuration
@EnableConfigurationProperties(BibliographicSelectionProperties.class)
public class BibliographicSelectionConfig {}
