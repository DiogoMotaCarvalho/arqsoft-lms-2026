package pt.psoft.g1.psoftg1.configuration;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA auditing and transaction-management configuration.
 *
 * @author pgsousa
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
@EnableTransactionManagement
public class JpaConfig {

  /**
   * in case there is no authenticated user, for instance during bootstrapping, we will write
   * SYSTEM.
   *
   * @return an {@code AuditorAware} that supplies the current auditor
   */
  @Bean("auditorProvider")
  public AuditorAware<String> auditorProvider() {
    return () ->
        Optional.ofNullable(SecurityContextHolder.getContext())
            .map(SecurityContext::getAuthentication)
            .map(Authentication::getName)
            .or(() -> Optional.of("SYSTEM"));
  }
}
