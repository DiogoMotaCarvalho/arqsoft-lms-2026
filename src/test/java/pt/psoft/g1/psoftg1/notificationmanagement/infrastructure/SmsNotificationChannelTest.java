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
 * Opaque-box tests (SUT equal to the adapter class) of the SMS mechanism (issue #20, ADR-0015,
 * criterion C3.1). The {@link SmsGateway} boundary is stubbed by a recording double, so each case
 * asserts the concrete SMS the adapter would send (recipient, body) rather than that the gateway was
 * merely invoked. Runs over plain objects with no Spring context.
 */
class SmsNotificationChannelTest {

  /** Records the messages handed to it so the mapped SMS can be asserted. */
  private static final class RecordingSmsGateway implements SmsGateway {
    private final List<SmsMessage> sent = new ArrayList<>();

    @Override
    public void send(SmsMessage message) {
      sent.add(message);
    }
  }

  private static Notification notification(String phone) {
    return new Notification(
        "2024/1",
        "reader@example.com",
        phone,
        LendingEventType.CREATED,
        "2024/15",
        LocalDate.of(2026, 1, 25),
        "New lending 2024/15",
        "Dear reader 2024/1, your lending 2024/15 was registered.");
  }

  @Test
  void mapsAFullNotificationOntoTheExpectedSms() {
    RecordingSmsGateway gateway = new RecordingSmsGateway();
    SmsNotificationChannel channel = new SmsNotificationChannel(gateway);

    channel.send(notification("912345678"));

    assertThat(gateway.sent).hasSize(1);
    SmsMessage sms = gateway.sent.get(0);
    assertThat(sms.getTo()).isEqualTo("912345678");
    assertThat(sms.getBody()).isEqualTo("Dear reader 2024/1, your lending 2024/15 was registered.");
  }

  @Test
  void reportsTheSmsMechanismName() {
    assertThat(new SmsNotificationChannel(new RecordingSmsGateway()).type()).isEqualTo("sms");
  }

  @Test
  void skipsQuietlyWhenTheReaderHasNoPhoneNumber() {
    RecordingSmsGateway gateway = new RecordingSmsGateway();
    SmsNotificationChannel channel = new SmsNotificationChannel(gateway);

    channel.send(notification(null));

    assertThat(gateway.sent).isEmpty();
  }

  @Test
  void aGatewayFailureIsReportedNotThrown() {
    SmsGateway failing =
        message -> {
          throw new SmsDeliveryException("vendor unavailable");
        };
    SmsNotificationChannel channel = new SmsNotificationChannel(failing);

    assertThatCode(() -> channel.send(notification("912345678"))).doesNotThrowAnyException();
  }
}
