package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.providers.googlebooks;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link GoogleBooksProperties} so the Google Books adapter's config binds at startup. */
@Configuration
@EnableConfigurationProperties(GoogleBooksProperties.class)
public class GoogleBooksConfig {}
