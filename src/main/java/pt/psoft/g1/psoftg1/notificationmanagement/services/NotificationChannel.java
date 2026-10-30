package pt.psoft.g1.psoftg1.notificationmanagement.services;

import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * The port a reader notification is delivered through, decoupled from any concrete mechanism (issue
 * #18, ADR-0014). Concrete mechanisms (email in #19, SMS in #20, webhook in #21) are adapters that
 * implement this one port, so a new mechanism is added as one adapter with no change to the lending
 * triggers or the existing mechanisms.
 *
 * <p>Channels are dumb: they deliver what they are given and hold no knowledge of lending events.
 * Delivery is best-effort: a channel with no usable address for the reader (for example SMS when the
 * reader has no phone) reports {@link DeliveryStatus#SKIPPED}, and an ordinary delivery failure is
 * reported as {@link DeliveryStatus#FAILED} rather than thrown, so the composite notifier (#22) can
 * audit the per-channel outcome and still fan out to the other channels.
 */
public interface NotificationChannel {

  /**
   * Delivers the notification through this mechanism and reports the outcome. A channel with no
   * usable address for the reader skips quietly ({@link DeliveryStatus#SKIPPED}); an ordinary
   * delivery failure is reported ({@link DeliveryStatus#FAILED}), not thrown.
   *
   * @param notification the rendered, recipient-bearing notification to deliver.
   * @return the outcome of the attempt.
   */
  DeliveryResult send(Notification notification);

  /**
   * This channel's stable mechanism name (for example {@code "email"}, {@code "sms"}, {@code
   * "webhook"}), mirroring {@code source()} on the bibliographic provider port (ADR-0012) so that
   * mechanism selection (#22) can refer to a mechanism by name.
   */
  String type();
}
