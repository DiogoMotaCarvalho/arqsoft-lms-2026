package pt.psoft.g1.psoftg1.bookmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;

/**
 * Context-binding test for the source selection configuration (#5, ADR-0012), in the style of the
 * lending-policy properties test. It asserts the setup-time outcome: a valid {@code
 * bibliographic.sources} list binds and the context starts, while a name with no matching provider
 * fails context startup (the fail-fast validation in the selecting provider's construction).
 */
class SelectingBibliographicProviderSelectionConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(StubProviders.class);

  @Test
  void aValidSourceListBindsAndTheContextStarts() {
    runner
        .withPropertyValues("bibliographic.sources=x,y")
        .run(ctx -> assertThat(ctx).hasNotFailed());
  }

  @Test
  void anEmptySourceListBindsAndDefaultsToAll() {
    runner.run(ctx -> assertThat(ctx).hasNotFailed());
  }

  @Test
  void anUnknownSourceNameFailsContextStartup() {
    runner
        .withPropertyValues("bibliographic.sources=does-not-exist")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  /** Two stub provider beans plus the selecting provider under the selection properties. */
  @EnableConfigurationProperties(BibliographicSelectionProperties.class)
  static class StubProviders {

    @Bean
    BibliographicProvider x() {
      return new InMemoryBibliographicProvider().named("x");
    }

    @Bean
    BibliographicProvider y() {
      return new InMemoryBibliographicProvider().named("y");
    }

    @Bean
    SelectingBibliographicProvider selector(
        List<BibliographicProvider> providers, BibliographicSelectionProperties properties) {
      return new SelectingBibliographicProvider(providers, properties);
    }
  }
}
