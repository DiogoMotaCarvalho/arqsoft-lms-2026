package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

/**
 * The concrete SMS an {@link SmsNotificationChannel} hands to an {@link SmsGateway} (issue #20,
 * ADR-0015): the recipient phone number and the short text body. SMS carries the body only, not a
 * subject. It is the shape the adapter maps a {@code Notification} onto, and what the opaque-box test
 * asserts, so the mapping is verifiable without any delivery. Immutable, both fields required and
 * non-blank.
 */
public final class SmsMessage {

  private final String to;
  private final String body;

  public SmsMessage(String to, String body) {
    this.to = requireNonBlank(to, "to");
    this.body = requireNonBlank(body, "body");
  }

  public String getTo() {
    return to;
  }

  public String getBody() {
    return body;
  }

  private static String requireNonBlank(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is required");
    }
    return value;
  }

  @Override
  public String toString() {
    return "SmsMessage{to=" + to + "}";
  }
}
