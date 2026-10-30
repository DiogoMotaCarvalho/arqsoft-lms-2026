package pt.psoft.g1.psoftg1.notificationmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEvent;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * Unit tests of the listener's fan-out and best-effort isolation (issue #18, ADR-0014), driven
 * through the package-private {@code dispatch} so no Spring context or transaction is needed. They
 * assert the delivered outcome: every channel receives the one rendered notification, and a channel
 * that throws neither stops the other channels nor propagates the failure to the caller (so a
 * mechanism being down never breaks the lending flow). The real {@link NotificationRenderer} is
 * used because it is a pure function; only the channels are test doubles.
 */
class LendingEventListenerTest {

  private final NotificationRenderer renderer = new NotificationRenderer();

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

  /** A channel that records what it was asked to deliver. */
  private static final class RecordingChannel implements NotificationChannel {
    private final List<Notification> delivered = new ArrayList<>();

    @Override
    public void send(Notification notification) {
      delivered.add(notification);
    }

    @Override
    public String type() {
      return "recording";
    }
  }

  /** A channel that always fails, standing in for a mechanism that is down. */
  private static final class FailingChannel implements NotificationChannel {
    @Override
    public void send(Notification notification) {
      throw new IllegalStateException("mechanism down");
    }

    @Override
    public String type() {
      return "failing";
    }
  }

  @Test
  void deliversTheSameRenderedNotificationToEveryChannel() {
    RecordingChannel first = new RecordingChannel();
    RecordingChannel second = new RecordingChannel();
    LendingEventListener listener = new LendingEventListener(renderer, List.of(first, second));

    listener.dispatch(anEvent());

    assertThat(first.delivered).hasSize(1);
    assertThat(second.delivered).hasSize(1);
    assertThat(first.delivered.get(0).getSubject()).isEqualTo("New lending 2024/15");
    assertThat(second.delivered.get(0)).isSameAs(first.delivered.get(0));
  }

  @Test
  void aFailingChannelDoesNotStopTheOthersNorPropagate() {
    RecordingChannel healthy = new RecordingChannel();
    LendingEventListener listener =
        new LendingEventListener(renderer, List.of(new FailingChannel(), healthy));

    assertThatCode(() -> listener.dispatch(anEvent())).doesNotThrowAnyException();
    assertThat(healthy.delivered).hasSize(1);
  }
}
