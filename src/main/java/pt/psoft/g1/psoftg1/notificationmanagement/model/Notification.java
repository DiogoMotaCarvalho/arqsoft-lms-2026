package pt.psoft.g1.psoftg1.notificationmanagement.model;

import java.time.LocalDate;
import java.util.Objects;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;

/**
 * The neutral reader notification produced once from a lending event (issue #18, ADR-0014) and
 * handed to every active {@code NotificationChannel}. It carries the recipient bundle (reader
 * number, email, phone), the structured event fields (the event type, the lending number, the
 * relevant limit date), and the rendered subject and body. Each channel takes the parts it needs:
 * an email channel uses the subject and body and the email address, a webhook channel the
 * structured fields. A channel with no usable address for the reader (for example SMS when the
 * reader has no phone) skips quietly.
 *
 * <p>This is a framework-free, immutable value object. The rendered subject and body are produced
 * by the renderer, not here, so the wording lives in one place.
 */
public final class Notification {

  private final String readerNumber;
  private final String email;
  private final String phone;
  private final LendingEventType eventType;
  private final String lendingNumber;
  private final LocalDate limitDate;
  private final String subject;
  private final String body;

  public Notification(
      String readerNumber,
      String email,
      String phone,
      LendingEventType eventType,
      String lendingNumber,
      LocalDate limitDate,
      String subject,
      String body) {
    this.readerNumber = Objects.requireNonNull(readerNumber, "readerNumber is required");
    this.email = email;
    this.phone = phone;
    this.eventType = Objects.requireNonNull(eventType, "eventType is required");
    this.lendingNumber = Objects.requireNonNull(lendingNumber, "lendingNumber is required");
    this.limitDate = limitDate;
    this.subject = Objects.requireNonNull(subject, "subject is required");
    this.body = Objects.requireNonNull(body, "body is required");
  }

  public String getReaderNumber() {
    return readerNumber;
  }

  /** The reader's email address, or {@code null} when none is on file. */
  public String getEmail() {
    return email;
  }

  /** The reader's phone number, or {@code null} when none is on file. */
  public String getPhone() {
    return phone;
  }

  public LendingEventType getEventType() {
    return eventType;
  }

  public String getLendingNumber() {
    return lendingNumber;
  }

  /** The lending's limit date, or {@code null} when the event does not carry one. */
  public LocalDate getLimitDate() {
    return limitDate;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }
}
