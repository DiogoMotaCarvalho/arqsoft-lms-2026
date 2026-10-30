package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * Opaque-box tests (SUT equal to the adapter class) of the email mechanism (issue #19, ADR-0015,
 * criterion C3.1). The {@link EmailGateway} boundary is stubbed by a recording double, so each case
 * asserts the concrete email the adapter would send (recipient, subject, body) rather than that the
 * gateway was merely invoked. Prior art: the bibliographic adapter class tests that stub the external
 * boundary. Runs over plain objects with no Spring context.
 */
class EmailNotificationChannelTest {

  /** Records the messages handed to it so the mapped email can be asserted. */
  private static final class RecordingEmailGateway implements EmailGateway {
    private final List<EmailMessage> sent = new ArrayList<>();

    @Override
    public void send(EmailMessage message) {
      sent.add(message);
    }
  }

  private static Notification notification(String email) {
    return new Notification(
        "2024/1",
        email,
        "912345678",
        LendingEventType.CREATED,
        "2024/15",
        LocalDate.of(2026, 1, 25),
        "New lending 2024/15",
        "Dear reader 2024/1, your lending 2024/15 was registered.");
  }

  @Test
  void mapsAFullNotificationOntoTheExpectedEmail() {
    RecordingEmailGateway gateway = new RecordingEmailGateway();
    EmailNotificationChannel channel = new EmailNotificationChannel(gateway);

    channel.send(notification("reader@example.com"));

    assertThat(gateway.sent).hasSize(1);
    EmailMessage email = gateway.sent.get(0);
    assertThat(email.getTo()).isEqualTo("reader@example.com");
    assertThat(email.getSubject()).isEqualTo("New lending 2024/15");
    assertThat(email.getBody())
        .isEqualTo("Dear reader 2024/1, your lending 2024/15 was registered.");
  }

  @Test
  void reportsTheEmailMechanismName() {
    assertThat(new EmailNotificationChannel(new RecordingEmailGateway()).type()).isEqualTo("email");
  }

  @Test
  void skipsQuietlyWhenTheReaderHasNoEmailAddress() {
    RecordingEmailGateway gateway = new RecordingEmailGateway();
    EmailNotificationChannel channel = new EmailNotificationChannel(gateway);

    channel.send(notification(null));

    assertThat(gateway.sent).isEmpty();
  }

  @Test
  void aGatewayFailureIsReportedNotThrown() {
    EmailGateway failing =
        message -> {
          throw new EmailDeliveryException("SMTP unavailable");
        };
    EmailNotificationChannel channel = new EmailNotificationChannel(failing);

    assertThatCode(() -> channel.send(notification("reader@example.com")))
        .doesNotThrowAnyException();
  }
}
