package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationChannel;

/**
 * Template for a notification channel that delivers through a gateway to an address carried on the
 * recipient bundle (issue #20, ADR-0015), shared by the email (#19) and SMS (#20) mechanisms. It
 * centralizes the two policies every gateway-backed mechanism shares and nothing mechanism-specific:
 *
 * <ul>
 *   <li>skip quietly when the reader has no address for this mechanism, so a missing contact is not
 *       an error;
 *   <li>treat delivery as best-effort, so a gateway failure is caught and reported rather than thrown
 *       through the after-commit listener (ADR-0014).
 * </ul>
 *
 * <p>A concrete mechanism supplies only the address it reads from the bundle and the actual delivery,
 * so it remains the one class that knows about its own mechanism and can be replaced in isolation.
 */
public abstract class AbstractGatewayNotificationChannel implements NotificationChannel {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public final void send(Notification notification) {
    final String address = recipientAddress(notification);
    if (address == null || address.isBlank()) {
      log.debug(
          "No {} address for reader {}; skipping {} notification",
          type(),
          notification.getReaderNumber(),
          type());
      return;
    }
    try {
      deliver(notification, address);
    } catch (RuntimeException e) {
      log.warn(
          "{} delivery failed for reader {} lending {}: {}",
          type(),
          notification.getReaderNumber(),
          notification.getLendingNumber(),
          e.getMessage());
    }
  }

  /**
   * The address this mechanism delivers to, taken from the recipient bundle on the notification (for
   * example the email or the phone number), or {@code null}/blank when the reader has none.
   */
  protected abstract String recipientAddress(Notification notification);

  /**
   * Performs the actual delivery of the notification to the resolved address. May throw, which the
   * template catches and reports as a best-effort failure.
   */
  protected abstract void deliver(Notification notification, String address);
}
