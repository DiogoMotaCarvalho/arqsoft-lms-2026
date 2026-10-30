package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationChannel;

/**
 * The email delivery mechanism (issue #19, ADR-0015): a thin adapter of the {@link
 * NotificationChannel} port that maps a {@link Notification} onto an {@link EmailMessage} and hands it
 * to the injected {@link EmailGateway}. It is the only code that knows about email, so the mechanism
 * can be removed or replaced in isolation.
 *
 * <p>It takes the email address from the recipient bundle on the notification (resolved upstream from
 * the account username) and the rendered prose subject and body, since the recipient is a person. A
 * notification with no email address is skipped quietly. Delivery is best-effort (ADR-0014): a
 * gateway failure is caught and reported, not thrown through the after-commit listener.
 */
@Component
public class EmailNotificationChannel implements NotificationChannel {

  /** This channel's stable mechanism name, the key selection (#22) uses to activate it. */
  static final String TYPE = "email";

  private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

  private final EmailGateway emailGateway;

  public EmailNotificationChannel(EmailGateway emailGateway) {
    this.emailGateway = emailGateway;
  }

  @Override
  public void send(Notification notification) {
    final String email = notification.getEmail();
    if (email == null || email.isBlank()) {
      log.debug(
          "No email address for reader {}; skipping email notification",
          notification.getReaderNumber());
      return;
    }

    final EmailMessage message =
        new EmailMessage(email, notification.getSubject(), notification.getBody());
    try {
      emailGateway.send(message);
    } catch (EmailDeliveryException e) {
      log.warn(
          "Email delivery failed for reader {} lending {}: {}",
          notification.getReaderNumber(),
          notification.getLendingNumber(),
          e.getMessage());
    }
  }

  @Override
  public String type() {
    return TYPE;
  }
}
