package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;

/**
 * The persisted delivery audit (issue #22, ADR-0016): one record per mechanism per lending event. A
 * fan-out to two active channels writes two records, so a run where one channel succeeds and
 * another fails keeps both outcomes. Both successes and failures are recorded, so the audit doubles
 * as the best-effort delivery log and a mechanism being down is visible rather than silent.
 *
 * <p>It is a JPA entity and therefore lives in the notification infrastructure, never in the
 * framework-free notification domain (enforced by ArchUnit). A skipped delivery (the reader had no
 * address for the mechanism) is not an attempt and is not recorded.
 */
@Entity
@Table(name = "NOTIFICATION_RECORD")
public class NotificationRecord {

  /** Whether a recorded delivery attempt succeeded or failed. A skip is never recorded. */
  public enum Outcome {
    SENT,
    FAILED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long pk;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Getter
  private LendingEventType eventType;

  @Column(nullable = false)
  @Getter
  private String mechanism;

  @Column(nullable = false)
  @Getter
  private String readerNumber;

  /** The address the mechanism targeted (email, phone, or endpoint); may be absent. */
  @Getter private String recipient;

  @Column(nullable = false)
  @Getter
  private String lendingNumber;

  @Column(nullable = false)
  @Getter
  private LocalDateTime timestamp;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Getter
  private Outcome outcome;

  /** The reason a delivery failed, or {@code null} for a successful delivery. */
  @Column(length = 1024)
  @Getter
  private String failureReason;

  protected NotificationRecord() {
    // for ORM only
  }

  public NotificationRecord(
      LendingEventType eventType,
      String mechanism,
      String readerNumber,
      String recipient,
      String lendingNumber,
      Outcome outcome,
      String failureReason) {
    this.eventType = eventType;
    this.mechanism = mechanism;
    this.readerNumber = readerNumber;
    this.recipient = recipient;
    this.lendingNumber = lendingNumber;
    this.timestamp = LocalDateTime.now();
    this.outcome = outcome;
    this.failureReason = failureReason;
  }
}
