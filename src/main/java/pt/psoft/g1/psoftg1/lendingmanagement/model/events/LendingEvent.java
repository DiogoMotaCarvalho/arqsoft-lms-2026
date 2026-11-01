package pt.psoft.g1.psoftg1.lendingmanagement.model.events;

import java.time.LocalDate;
import java.util.Objects;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;

/**
 * An immutable snapshot of a lending event (issue #18, ADR-0014), published by {@code
 * LendingServiceImpl} and the overdue sweep through Spring's {@code ApplicationEventPublisher} and
 * consumed by the notification context. It is a self-contained value, not the persistent {@code
 * Lending} entity, because the notification listener runs after the transaction has committed and
 * possibly on another thread, where a live entity would be detached. It therefore resolves the
 * reader's contacts (email is the account username, phone is held on the reader details) at publish
 * time and carries the lending identity and the dates a notification may need.
 *
 * <p>This type is framework-free: it is the one-way edge from {@code notificationmanagement} back
 * to {@code lendingmanagement} and carries no Spring or JPA dependency itself.
 */
public final class LendingEvent {

  private final LendingEventType type;
  private final String readerNumber;
  private final String email;
  private final String phone;
  private final String lendingNumber;
  private final LocalDate startDate;
  private final LocalDate limitDate;
  private final LocalDate returnedDate;

  public LendingEvent(
      LendingEventType type,
      String readerNumber,
      String email,
      String phone,
      String lendingNumber,
      LocalDate startDate,
      LocalDate limitDate,
      LocalDate returnedDate) {
    this.type = Objects.requireNonNull(type, "type is required");
    this.readerNumber = Objects.requireNonNull(readerNumber, "readerNumber is required");
    this.email = email;
    this.phone = phone;
    this.lendingNumber = Objects.requireNonNull(lendingNumber, "lendingNumber is required");
    this.startDate = startDate;
    this.limitDate = limitDate;
    this.returnedDate = returnedDate;
  }

  /**
   * Snapshots the given lending for the given event type, resolving the recipient contacts from the
   * reader details at publish time. Used by the lending flow and the overdue sweep so the
   * resolution lives in one place.
   */
  public static LendingEvent of(LendingEventType type, Lending lending) {
    final var reader = lending.getReaderDetails();
    return new LendingEvent(
        type,
        lending.getReaderNumber(),
        reader.getReader().getUsername(),
        reader.getPhoneNumber(),
        lending.getLendingNumber(),
        lending.getStartDate(),
        lending.getLimitDate(),
        lending.getReturnedDate());
  }

  public LendingEventType getType() {
    return type;
  }

  public String getReaderNumber() {
    return readerNumber;
  }

  /** The reader's email (the account username), or {@code null} when none is on file. */
  public String getEmail() {
    return email;
  }

  /** The reader's phone number, or {@code null} when none is on file. */
  public String getPhone() {
    return phone;
  }

  public String getLendingNumber() {
    return lendingNumber;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getLimitDate() {
    return limitDate;
  }

  /** The date the lending was returned, or {@code null} when it has not been returned. */
  public LocalDate getReturnedDate() {
    return returnedDate;
  }
}
