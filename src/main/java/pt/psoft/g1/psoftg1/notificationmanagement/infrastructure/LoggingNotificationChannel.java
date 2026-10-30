package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationChannel;

/**
 * The only notification mechanism present in #18: a stub channel that logs the rendered
 * notification (ADR-0014). It exists so the event-to-notification flow is exercisable and
 * demonstrable end to end before the real mechanisms arrive, and so the listener always has at
 * least one channel to fan out to. It is an ordinary adapter of the {@link NotificationChannel}
 * port, which is the evidence that the real mechanisms (#19 email, #20 SMS, #21 webhook) are each
 * added as one adapter with no change to the triggers or this listener.
 */
@Component
public class LoggingNotificationChannel implements NotificationChannel {

  private static final Logger log = LoggerFactory.getLogger(LoggingNotificationChannel.class);

  @Override
  public void send(Notification notification) {
    log.info(
        "[{}] notification to reader {}: {} | {}",
        notification.getEventType(),
        notification.getReaderNumber(),
        notification.getSubject(),
        notification.getBody());
  }

  @Override
  public String type() {
    return "log";
  }
}
