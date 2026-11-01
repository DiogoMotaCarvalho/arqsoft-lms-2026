package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

/**
 * Raised by an {@link EmailGateway} when it cannot deliver an email (issue #19, ADR-0015). The
 * {@link EmailNotificationChannel} catches it so the failure is reported rather than thrown through
 * the after-commit listener, keeping delivery best-effort (ADR-0014).
 */
public class EmailDeliveryException extends RuntimeException {

  public EmailDeliveryException(String message, Throwable cause) {
    super(message, cause);
  }

  public EmailDeliveryException(String message) {
    super(message);
  }
}
