package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the webhook mechanism (issue #21, ADR-0015), bound from the {@code
 * notifications.webhook} prefix, in the same style as the bibliographic adapter properties
 * (ADR-0010). The endpoint is a property of the mechanism, not of any reader, so it is pointed at a
 * different target per deployment with no code change. When the base URL is blank no endpoint is
 * configured and the adapter skips. The timeout bounds the single POST.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "notifications.webhook")
public class WebhookProperties {

  /** Endpoint the webhook POSTs to. Blank means no endpoint is configured and delivery is skipped. */
  private String baseUrl = "";

  /** Timeout for the single POST call. */
  private Duration timeout = Duration.ofSeconds(5);
}
