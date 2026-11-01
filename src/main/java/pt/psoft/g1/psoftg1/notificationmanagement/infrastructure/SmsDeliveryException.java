package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

/**
 * Raised by an {@link SmsGateway} when it cannot deliver an SMS (issue #20, ADR-0015). The {@link
 * SmsNotificationChannel} (through its template base) catches it so the failure is reported rather
 * than thrown through the after-commit listener, keeping delivery best-effort (ADR-0014).
 */
public class SmsDeliveryException extends RuntimeException {

  public SmsDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }

  public SmsDeliveryException(String message) {
    super(message);
  }
}
