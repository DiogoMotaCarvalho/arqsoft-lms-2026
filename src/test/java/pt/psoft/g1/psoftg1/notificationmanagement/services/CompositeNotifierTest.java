package pt.psoft.g1.psoftg1.notificationmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.notificationmanagement.infrastructure.NotificationRecord;
import pt.psoft.g1.psoftg1.notificationmanagement.infrastructure.NotificationRecordRepository;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * Unit tests of the composite notifier (issue #22, ADR-0016). They assert the outcomes that matter:
 * which mechanisms are sent on, in what selection, and what audit records result, with the channels
 * as configurable doubles and the audit repository mocked. They cover fan-out to several active
 * mechanisms, the empty-selection default, fail-fast on an unknown name, the global disable switch,
 * that a skip is not recorded while a failure is recorded with its reason, and that one channel's
 * fault does not stop the others.
 */
class CompositeNotifierTest {

  private final NotificationRecordRepository repository = mock(NotificationRecordRepository.class);

  /** A channel double with a fixed type that returns a fixed result (or throws a fault). */
  private static final class StubChannel implements NotificationChannel {
    private final String type;
    private final DeliveryResult result;
    private final boolean fault;
    private int sends;

    StubChannel(String type, DeliveryResult result) {
      this(type, result, false);
    }

    StubChannel(String type, DeliveryResult result, boolean fault) {
      this.type = type;
      this.result = result;
      this.fault = fault;
    }

    @Override
    public DeliveryResult send(Notification notification) {
      sends++;
      if (fault) {
        throw new RuntimeException("unexpected fault");
      }
      return result;
    }

    @Override
    public String type() {
      return type;
    }
  }

  private static Notification notification() {
    return new Notification(
        "2024/1",
        "reader@example.com",
        "912345678",
        LendingEventType.CREATED,
        "2024/15",
        LocalDate.of(2026, 1, 25),
        "New lending 2024/15",
        "body");
  }

  private static NotificationSelectionProperties props(boolean enabled, String... channels) {
    NotificationSelectionProperties p = new NotificationSelectionProperties();
    p.setEnabled(enabled);
    p.setChannels(List.of(channels));
    return p;
  }

  private List<NotificationRecord> savedRecords(int expected) {
    ArgumentCaptor<NotificationRecord> captor = ArgumentCaptor.forClass(NotificationRecord.class);
    verify(repository, org.mockito.Mockito.times(expected)).save(captor.capture());
    return captor.getAllValues();
  }

  @Test
  void fansOutToEveryActiveMechanismAndRecordsEachAsSent() {
    StubChannel email = new StubChannel("email", DeliveryResult.sent("reader@example.com"));
    StubChannel sms = new StubChannel("sms", DeliveryResult.sent("912345678"));
    CompositeNotifier notifier =
        new CompositeNotifier(List.of(email, sms), props(true, "email", "sms"), repository);

    notifier.notify(notification());

    assertThat(email.sends).isEqualTo(1);
    assertThat(sms.sends).isEqualTo(1);
    List<NotificationRecord> records = savedRecords(2);
    assertThat(records)
        .extracting(NotificationRecord::getMechanism)
        .containsExactlyInAnyOrder("email", "sms");
    assertThat(records)
        .allSatisfy(r -> assertThat(r.getOutcome()).isEqualTo(NotificationRecord.Outcome.SENT));
    assertThat(records).allSatisfy(r -> assertThat(r.getLendingNumber()).isEqualTo("2024/15"));
  }

  @Test
  void activatesOnlyTheConfiguredMechanisms() {
    StubChannel email = new StubChannel("email", DeliveryResult.sent("reader@example.com"));
    StubChannel sms = new StubChannel("sms", DeliveryResult.sent("912345678"));
    CompositeNotifier notifier =
        new CompositeNotifier(List.of(email, sms), props(true, "email"), repository);

    notifier.notify(notification());

    assertThat(email.sends).isEqualTo(1);
    assertThat(sms.sends).isZero();
    savedRecords(1);
  }

  @Test
  void anEmptySelectionActivatesAllRegisteredChannels() {
    StubChannel email = new StubChannel("email", DeliveryResult.sent("reader@example.com"));
    StubChannel sms = new StubChannel("sms", DeliveryResult.sent("912345678"));
    CompositeNotifier notifier =
        new CompositeNotifier(List.of(email, sms), props(true), repository);

    notifier.notify(notification());

    assertThat(email.sends).isEqualTo(1);
    assertThat(sms.sends).isEqualTo(1);
  }

  @Test
  void anUnknownChannelNameFailsFastAtConstruction() {
    StubChannel email = new StubChannel("email", DeliveryResult.sent("reader@example.com"));

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> new CompositeNotifier(List.of(email), props(true, "carrier-pigeon"), repository))
        .withMessageContaining("carrier-pigeon");
  }

  @Test
  void theGlobalSwitchDisablesAllDeliveryAndRecording() {
    StubChannel email = new StubChannel("email", DeliveryResult.sent("reader@example.com"));
    CompositeNotifier notifier =
        new CompositeNotifier(List.of(email), props(false, "email"), repository);

    notifier.notify(notification());

    assertThat(email.sends).isZero();
    verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void aSkippedDeliveryIsNotRecorded() {
    StubChannel sms = new StubChannel("sms", DeliveryResult.skipped());
    CompositeNotifier notifier =
        new CompositeNotifier(List.of(sms), props(true, "sms"), repository);

    notifier.notify(notification());

    assertThat(sms.sends).isEqualTo(1);
    verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void aFailedDeliveryIsRecordedWithItsReason() {
    StubChannel webhook =
        new StubChannel("webhook", DeliveryResult.failed("http://hook", "503 from endpoint"));
    CompositeNotifier notifier =
        new CompositeNotifier(List.of(webhook), props(true, "webhook"), repository);

    notifier.notify(notification());

    NotificationRecord record = savedRecords(1).get(0);
    assertThat(record.getMechanism()).isEqualTo("webhook");
    assertThat(record.getOutcome()).isEqualTo(NotificationRecord.Outcome.FAILED);
    assertThat(record.getFailureReason()).isEqualTo("503 from endpoint");
    assertThat(record.getRecipient()).isEqualTo("http://hook");
  }

  @Test
  void oneChannelFaultIsIsolatedSoTheOthersStillRun() {
    StubChannel faulty = new StubChannel("email", DeliveryResult.sent("x"), true);
    StubChannel healthy = new StubChannel("sms", DeliveryResult.sent("912345678"));
    CompositeNotifier notifier =
        new CompositeNotifier(List.of(faulty, healthy), props(true, "email", "sms"), repository);

    notifier.notify(notification());

    assertThat(healthy.sends).isEqualTo(1);
    List<NotificationRecord> records = savedRecords(2);
    assertThat(records)
        .anySatisfy(
            r -> {
              assertThat(r.getMechanism()).isEqualTo("email");
              assertThat(r.getOutcome()).isEqualTo(NotificationRecord.Outcome.FAILED);
            })
        .anySatisfy(
            r -> {
              assertThat(r.getMechanism()).isEqualTo("sms");
              assertThat(r.getOutcome()).isEqualTo(NotificationRecord.Outcome.SENT);
            });
  }
}
