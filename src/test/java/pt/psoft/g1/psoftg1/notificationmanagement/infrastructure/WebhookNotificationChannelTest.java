package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.reactive.MockClientHttpRequest;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;
import reactor.core.publisher.Mono;

/**
 * Opaque-box tests (SUT equal to the adapter class) of the webhook mechanism (issue #21, ADR-0015,
 * criterion C3.1). The HTTP exchange is stubbed with an {@link ExchangeFunction} that records the
 * outgoing {@link ClientRequest} and returns a chosen status, exactly as the bibliographic adapters
 * stub their outbound calls. Each case asserts the request the adapter actually makes (the POST URL
 * and the JSON body) and its handling of the response, with no network.
 */
class WebhookNotificationChannelTest {

  private static final String URL = "http://hooks.example.com/lending";

  private static Notification notification() {
    return new Notification(
        "2024/1",
        "reader@example.com",
        "912345678",
        LendingEventType.CREATED,
        "2024/15",
        LocalDate.of(2026, 1, 25),
        "New lending 2024/15",
        "Dear reader 2024/1, your lending 2024/15 was registered.");
  }

  private static WebhookNotificationChannel channel(
      HttpStatus status, AtomicReference<ClientRequest> captured, String baseUrl) {
    ExchangeFunction stub =
        request -> {
          captured.set(request);
          return Mono.just(ClientResponse.create(status).build());
        };
    WebClient.Builder builder = WebClient.builder().exchangeFunction(stub);
    WebhookProperties properties = new WebhookProperties();
    properties.setBaseUrl(baseUrl);
    return new WebhookNotificationChannel(builder, properties);
  }

  private static String bodyOf(ClientRequest request) {
    MockClientHttpRequest mock = new MockClientHttpRequest(HttpMethod.POST, "/");
    request.writeTo(mock, ExchangeStrategies.withDefaults()).block();
    return mock.getBodyAsString().block();
  }

  @Test
  void postsTheStructuredPayloadToTheConfiguredUrl() {
    AtomicReference<ClientRequest> captured = new AtomicReference<>();

    channel(HttpStatus.OK, captured, URL).send(notification());

    ClientRequest request = captured.get();
    assertThat(request).isNotNull();
    assertThat(request.method()).isEqualTo(HttpMethod.POST);
    assertThat(request.url().toString()).isEqualTo(URL);
    assertThat(bodyOf(request))
        .contains("\"eventType\":\"CREATED\"")
        .contains("\"readerNumber\":\"2024/1\"")
        .contains("\"lendingNumber\":\"2024/15\"")
        .contains("\"limitDate\":\"2026-01-25\"");
  }

  @Test
  void sendsTheStructuredFieldsAndNotTheRenderedProse() {
    AtomicReference<ClientRequest> captured = new AtomicReference<>();

    channel(HttpStatus.OK, captured, URL).send(notification());

    assertThat(bodyOf(captured.get()))
        .doesNotContain("Dear reader")
        .doesNotContain("New lending 2024/15");
  }

  @Test
  void reportsTheWebhookMechanismName() {
    assertThat(channel(HttpStatus.OK, new AtomicReference<>(), URL).type()).isEqualTo("webhook");
  }

  @Test
  void skipsQuietlyWhenNoEndpointIsConfigured() {
    AtomicReference<ClientRequest> captured = new AtomicReference<>();

    channel(HttpStatus.OK, captured, "").send(notification());

    assertThat(captured.get()).isNull();
  }

  @Test
  void aFailedPostIsReportedNotThrown() {
    WebhookNotificationChannel channel =
        channel(HttpStatus.INTERNAL_SERVER_ERROR, new AtomicReference<>(), URL);

    assertThatCode(() -> channel.send(notification())).doesNotThrowAnyException();
  }
}
