package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationChannel;

/**
 * The SMS delivery mechanism (issue #20, ADR-0015): a thin adapter of the {@link NotificationChannel}
 * port that maps a {@link Notification} onto an {@link SmsMessage} and hands it to the injected {@link
 * SmsGateway}. It is the only code that knows about SMS, so the mechanism can be removed or replaced
 * in isolation.
 *
 * <p>It takes the phone number from the recipient bundle and uses the rendered prose body (SMS sends
 * the body, not the subject). The skip-when-absent and best-effort policies live in {@link
 * AbstractGatewayNotificationChannel}.
 */
@Component
public class SmsNotificationChannel extends AbstractGatewayNotificationChannel {

  /** This channel's stable mechanism name, the key selection (#22) uses to activate it. */
  static final String TYPE = "sms";

  private final SmsGateway smsGateway;

  public SmsNotificationChannel(SmsGateway smsGateway) {
    this.smsGateway = smsGateway;
  }

  @Override
  protected String recipientAddress(Notification notification) {
    return notification.getPhone();
  }

  @Override
  protected void deliver(Notification notification, String address) {
    smsGateway.send(new SmsMessage(address, notification.getBody()));
  }

  @Override
  public String type() {
    return TYPE;
  }
}
