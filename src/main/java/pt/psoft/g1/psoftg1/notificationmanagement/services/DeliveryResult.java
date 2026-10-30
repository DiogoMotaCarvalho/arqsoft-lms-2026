package pt.psoft.g1.psoftg1.notificationmanagement.services;

/**
 * The result a {@link NotificationChannel} returns for one delivery attempt (issue #22, ADR-0016).
 * It carries the {@link DeliveryStatus}, the recipient address the channel targeted (the email, the
 * phone, or the webhook endpoint; {@code null} when skipped), and, for a failure, the reason. The
 * channel never throws for an ordinary delivery failure: it reports it here so the composite notifier
 * can record the outcome and still fan out to the other channels (best-effort, ADR-0014).
 */
public record DeliveryResult(DeliveryStatus status, String recipient, String failureReason) {

  public static DeliveryResult sent(String recipient) {
    return new DeliveryResult(DeliveryStatus.SENT, recipient, null);
  }

  public static DeliveryResult skipped() {
    return new DeliveryResult(DeliveryStatus.SKIPPED, null, null);
  }

  public static DeliveryResult failed(String recipient, String failureReason) {
    return new DeliveryResult(DeliveryStatus.FAILED, recipient, failureReason);
  }
}
