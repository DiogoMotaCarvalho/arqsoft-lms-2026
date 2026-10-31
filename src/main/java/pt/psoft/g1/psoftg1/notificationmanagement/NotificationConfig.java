package pt.psoft.g1.psoftg1.notificationmanagement;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationRenderer;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationSelectionProperties;

/**
 * Wires the notification context (issue #18, ADR-0014; extended in #22, ADR-0016). It enables
 * asynchronous execution so the after-commit {@code LendingEventListener} runs off the request
 * thread, registers the framework-free {@link NotificationRenderer} as a bean so the renderer
 * itself carries no Spring dependency, and binds the mechanism-selection configuration ({@link
 * NotificationSelectionProperties}).
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(NotificationSelectionProperties.class)
public class NotificationConfig {

  @Bean
  public NotificationRenderer notificationRenderer() {
    return new NotificationRenderer();
  }
}
