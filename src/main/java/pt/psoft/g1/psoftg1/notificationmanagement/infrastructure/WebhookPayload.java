package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * The structured JSON body the webhook mechanism POSTs (issue #21, ADR-0015). It carries the
 * notification's structured event fields (the event type, the reader number, the lending number,
 * and the relevant limit date), not the rendered prose, because a webhook consumer is a machine
 * that parses fields rather than sentences. Dates are serialized as ISO-8601 strings so the payload
 * does not depend on a JSON date module, and an absent date is {@code null}.
 */
public record WebhookPayload(
    String eventType, String readerNumber, String lendingNumber, String limitDate) {

  /** Builds the payload from a notification, taking only the structured fields a machine needs. */
  public static WebhookPayload from(Notification notification) {
    return new WebhookPayload(
        notification.getEventType().name(),
        notification.getReaderNumber(),
        notification.getLendingNumber(),
        notification.getLimitDate() == null ? null : notification.getLimitDate().toString());
  }
}
