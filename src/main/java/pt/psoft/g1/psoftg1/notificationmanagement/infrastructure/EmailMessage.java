package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

/**
 * The concrete email an {@link EmailNotificationChannel} hands to an {@link EmailGateway} (issue
 * #19, ADR-0015): the recipient address and the rendered prose subject and body. It is the shape
 * the adapter maps a {@code Notification} onto, and it is what the opaque-box test asserts, so the
 * mapping is verifiable without any delivery. Immutable, all fields required and non-blank.
 */
public final class EmailMessage {

  private final String to;
  private final String subject;
  private final String body;

  public EmailMessage(String to, String subject, String body) {
    this.to = requireNonBlank(to, "to");
    this.subject = requireNonBlank(subject, "subject");
    this.body = requireNonBlank(body, "body");
  }

  public String getTo() {
    return to;
  }

  public String getSubject() {
    return subject;
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
    return "EmailMessage{to=" + to + ", subject=" + subject + "}";
  }
}
