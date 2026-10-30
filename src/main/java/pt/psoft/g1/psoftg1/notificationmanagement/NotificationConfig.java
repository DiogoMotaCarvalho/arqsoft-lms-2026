package pt.psoft.g1.psoftg1.notificationmanagement;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationRenderer;

/**
 * Wires the notification context (issue #18, ADR-0014). It enables asynchronous execution so the
 * after-commit {@code LendingEventListener} runs off the request thread, and it registers the
 * framework-free {@link NotificationRenderer} as a bean so the renderer itself carries no Spring
 * dependency.
 */
@Configuration
@EnableAsync
public class NotificationConfig {

  @Bean
  public NotificationRenderer notificationRenderer() {
    return new NotificationRenderer();
  }
}
