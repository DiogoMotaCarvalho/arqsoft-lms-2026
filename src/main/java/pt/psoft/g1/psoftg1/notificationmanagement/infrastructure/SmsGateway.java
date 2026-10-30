package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

/**
 * The seam between the {@link SmsNotificationChannel} and the actual SMS delivery (issue #20,
 * ADR-0015). The adapter maps a {@code Notification} onto an {@link SmsMessage} and hands it here, so
 * the mapping is the behaviour under test and the send is the stubbed boundary. The default
 * implementation ({@link LoggingSmsGateway}) is self-contained, so the feature runs with no SMS
 * vendor account; a real vendor API is a later alternative implementation selected by configuration,
 * with no change to the adapter.
 */
public interface SmsGateway {

  /**
   * Delivers the SMS.
   *
   * @param message the recipient phone number and body to send.
   * @throws SmsDeliveryException if the SMS cannot be delivered.
   */
  void send(SmsMessage message);
}
