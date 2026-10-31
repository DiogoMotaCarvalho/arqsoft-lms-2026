package pt.psoft.g1.psoftg1.notificationmanagement.services;

/**
 * The outcome a {@link NotificationChannel} reports for one delivery attempt (issue #22, ADR-0016),
 * so the composite notifier can audit per-channel results.
 *
 * <ul>
 *   <li>{@code SENT}: the channel delivered the notification.
 *   <li>{@code SKIPPED}: the channel had no usable address for the reader, so it is not applicable
 *       (a quiet skip, not an error, and not audited).
 *   <li>{@code FAILED}: the channel attempted delivery and it failed (audited with a reason).
 * </ul>
 */
public enum DeliveryStatus {
  SENT,
  SKIPPED,
  FAILED
}
