package pt.psoft.g1.psoftg1.notificationmanagement.services;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEvent;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * Listens for a {@link LendingEvent} and delivers the resulting notification (issue #18, ADR-0014).
 * It is bound to the after-commit phase and runs asynchronously, so a notification fires only once
 * the lending transaction has committed and never blocks the request or rolls a lending back. It
 * renders the event once and then fans the single {@link Notification} out to every active {@link
 * NotificationChannel}, isolating each channel so one mechanism failing (or absent) does not stop
 * the others. In #18 the only registered channel is a logging stub; the real mechanisms (#19 to
 * #21) and the selection and combination (#22) add channels without touching this listener or the
 * triggers.
 */
@Component
public class LendingEventListener {

  private static final Logger log = LoggerFactory.getLogger(LendingEventListener.class);

  private final NotificationRenderer renderer;
  private final List<NotificationChannel> channels;

  public LendingEventListener(NotificationRenderer renderer, List<NotificationChannel> channels) {
    this.renderer = renderer;
    this.channels = channels;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onLendingEvent(LendingEvent event) {
    dispatch(event);
  }

  /**
   * Renders the event once and delivers to every channel best-effort, isolating a channel failure
   * so the remaining channels still run and the failure never propagates to the caller.
   * Package-private so the fan-out can be unit-tested without a Spring context or a transaction.
   */
  void dispatch(LendingEvent event) {
    final Notification notification = renderer.render(event);
    for (NotificationChannel channel : channels) {
      try {
        channel.send(notification);
      } catch (RuntimeException e) {
        log.warn(
            "Notification channel '{}' failed for lending {}: {}",
            channel.type(),
            event.getLendingNumber(),
            e.getMessage());
      }
    }
  }
}
