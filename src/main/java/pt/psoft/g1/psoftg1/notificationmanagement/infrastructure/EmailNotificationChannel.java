package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationChannel;

/**
 * The email delivery mechanism (issue #19, ADR-0015): a thin adapter of the {@link
 * NotificationChannel} port that maps a {@link Notification} onto an {@link EmailMessage} and hands
 * it to the injected {@link EmailGateway}. It is the only code that knows about email, so the
 * mechanism can be removed or replaced in isolation.
 *
 * <p>It takes the email address from the recipient bundle (resolved upstream from the account
 * username) and the rendered prose subject and body, since the recipient is a person. The
 * skip-when-absent and best-effort policies live in {@link AbstractGatewayNotificationChannel}.
 */
@Component
public class EmailNotificationChannel extends AbstractGatewayNotificationChannel {

  /** This channel's stable mechanism name, the key selection (#22) uses to activate it. */
  static final String TYPE = "email";

  private final EmailGateway emailGateway;

  public EmailNotificationChannel(EmailGateway emailGateway) {
    this.emailGateway = emailGateway;
  }

  @Override
  protected String recipientAddress(Notification notification) {
    return notification.getEmail();
  }

  @Override
  protected void deliver(Notification notification, String address) {
    emailGateway.send(new EmailMessage(address, notification.getSubject(), notification.getBody()));
  }

  @Override
  public String type() {
    return TYPE;
  }
}
