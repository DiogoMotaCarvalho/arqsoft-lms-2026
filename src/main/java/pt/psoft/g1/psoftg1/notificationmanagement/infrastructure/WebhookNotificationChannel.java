package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;
import pt.psoft.g1.psoftg1.notificationmanagement.services.DeliveryResult;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationChannel;

/**
 * The webhook delivery mechanism (issue #21, ADR-0015): a thin adapter of the {@link
 * NotificationChannel} port that POSTs a structured JSON payload to a setup-time-configured endpoint
 * through {@link WebClient}. It is the only code that knows about HTTP delivery, so the mechanism can
 * be replaced in isolation.
 *
 * <p>The payload is built from the notification's structured event fields ({@link WebhookPayload}),
 * not the rendered prose, since a webhook consumer is a machine. The endpoint is a property of the
 * mechanism, not of the reader; when no endpoint is configured the adapter skips quietly. Delivery is
 * best-effort (ADR-0014): a failed POST (non-2xx, timeout, connection error) is caught and reported,
 * not thrown through the after-commit listener.
 */
@Component
public class WebhookNotificationChannel implements NotificationChannel {

  /** This channel's stable mechanism name, the key selection (#22) uses to activate it. */
  static final String TYPE = "webhook";

  private final WebClient webClient;
  private final String baseUrl;
  private final Duration timeout;

  public WebhookNotificationChannel(WebClient.Builder webClientBuilder, WebhookProperties properties) {
    this.webClient = webClientBuilder.build();
    this.baseUrl = properties.getBaseUrl();
    this.timeout = properties.getTimeout();
  }

  @Override
  public DeliveryResult send(Notification notification) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return DeliveryResult.skipped();
    }

    final WebhookPayload payload = WebhookPayload.from(notification);
    try {
      webClient
          .post()
          .uri(baseUrl)
          .contentType(MediaType.APPLICATION_JSON)
          .bodyValue(payload)
          .retrieve()
          .toBodilessEntity()
          .timeout(timeout)
          .block();
      return DeliveryResult.sent(baseUrl);
    } catch (RuntimeException e) {
      return DeliveryResult.failed(baseUrl, e.getMessage());
    }
  }

  @Override
  public String type() {
    return TYPE;
  }
}
