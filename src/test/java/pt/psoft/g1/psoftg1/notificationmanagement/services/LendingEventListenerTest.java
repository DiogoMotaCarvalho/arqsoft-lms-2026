package pt.psoft.g1.psoftg1.notificationmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEvent;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * Unit test of the listener's render-and-delegate step (issue #18, ADR-0014; #22, ADR-0016), driven
 * through the package-private {@code dispatch} so no Spring context or transaction is needed. It
 * asserts the outcome the listener is responsible for: the event is rendered once and the resulting
 * {@link Notification}, with the right content and recipient bundle, is handed to the {@link
 * Notifier}. The fan-out, selection, and audit are the composite notifier's responsibility and are
 * tested in {@code CompositeNotifierTest}; here the notifier is a recording double.
 */
class LendingEventListenerTest {

  private final NotificationRenderer renderer = new NotificationRenderer();

  /** Records the notifications handed to it. */
  private static final class RecordingNotifier implements Notifier {
    private final List<Notification> received = new ArrayList<>();

    @Override
    public void notify(Notification notification) {
      received.add(notification);
    }
  }

  private static LendingEvent anEvent() {
    return new LendingEvent(
        LendingEventType.CREATED,
        "2024/1",
        "reader@example.com",
        "912345678",
        "2024/15",
        LocalDate.of(2026, 1, 10),
        LocalDate.of(2026, 1, 25),
        null);
  }

  @Test
  void rendersTheEventOnceAndHandsTheNotificationToTheNotifier() {
    RecordingNotifier notifier = new RecordingNotifier();
    LendingEventListener listener = new LendingEventListener(renderer, notifier);

    listener.dispatch(anEvent());

    assertThat(notifier.received).hasSize(1);
    Notification notification = notifier.received.get(0);
    assertThat(notification.getEventType()).isEqualTo(LendingEventType.CREATED);
    assertThat(notification.getLendingNumber()).isEqualTo("2024/15");
    assertThat(notification.getReaderNumber()).isEqualTo("2024/1");
    assertThat(notification.getEmail()).isEqualTo("reader@example.com");
    assertThat(notification.getPhone()).isEqualTo("912345678");
    assertThat(notification.getSubject()).isEqualTo("New lending 2024/15");
  }
}
