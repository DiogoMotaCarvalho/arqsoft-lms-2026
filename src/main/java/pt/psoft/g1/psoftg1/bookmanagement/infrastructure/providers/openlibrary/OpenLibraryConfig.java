package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.providers.openlibrary;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link OpenLibraryProperties} so the Open Library adapter's config binds at startup.
 */
@Configuration
@EnableConfigurationProperties(OpenLibraryProperties.class)
public class OpenLibraryConfig {}
