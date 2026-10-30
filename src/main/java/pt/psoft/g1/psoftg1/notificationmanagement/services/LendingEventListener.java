package pt.psoft.g1.psoftg1.notificationmanagement.services;

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
 * renders the event once and hands the single {@link Notification} to the {@link Notifier}, which
 * applies the mechanism selection, fans out to the active channels, and audits each attempt (#22,
 * ADR-0016). The listener depends only on the renderer and the notifier, so adding or selecting
 * mechanisms never touches it.
 */
@Component
public class LendingEventListener {

  private final NotificationRenderer renderer;
  private final Notifier notifier;

  public LendingEventListener(NotificationRenderer renderer, Notifier notifier) {
    this.renderer = renderer;
    this.notifier = notifier;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onLendingEvent(LendingEvent event) {
    dispatch(event);
  }

  /**
   * Renders the event once and hands the notification to the notifier. Package-private so the
   * render-and-delegate step can be unit-tested without a Spring context or a transaction.
   */
  void dispatch(LendingEvent event) {
    final Notification notification = renderer.render(event);
    notifier.notify(notification);
  }
}
