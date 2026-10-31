package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

/**
 * The seam between the {@link EmailNotificationChannel} and the actual email delivery (issue #19,
 * ADR-0015). The adapter maps a {@code Notification} onto an {@link EmailMessage} and hands it
 * here, so the mapping is the behaviour under test and the send is the stubbed boundary. The
 * default implementation ({@link LoggingEmailGateway}) is self-contained, so the feature runs with
 * no SMTP server; a real SMTP sender is a later alternative implementation selected by
 * configuration, with no change to the adapter.
 */
public interface EmailGateway {

  /**
   * Delivers the email.
   *
   * @param message the recipient, subject, and body to send.
   * @throws EmailDeliveryException if the email cannot be delivered.
   */
  void send(EmailMessage message);
}
