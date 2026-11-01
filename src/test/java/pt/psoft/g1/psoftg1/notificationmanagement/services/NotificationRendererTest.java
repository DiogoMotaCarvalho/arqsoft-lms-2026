package pt.psoft.g1.psoftg1.notificationmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEvent;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * Transparent-box tests (SUT equal to domain class) for the event-to-notification mapping (issue
 * #18, ADR-0014, criterion C3.2). One case per event type, each asserting the rendered subject and
 * body and the recipient bundle carried onto the notification, not merely that a branch ran. The
 * expected wording is written as independent literals so the assertion cannot agree with the code
 * by construction. Runs over plain objects with no Spring context, as {@code
 * LendingPolicySelectorTest} does for the policy-selection decision.
 */
class NotificationRendererTest {

  private static final String READER = "2024/1";
  private static final String EMAIL = "reader@example.com";
  private static final String PHONE = "912345678";
  private static final String LENDING = "2024/15";
  private static final LocalDate START = LocalDate.of(2026, 1, 10);
  private static final LocalDate LIMIT = LocalDate.of(2026, 1, 25);
  private static final LocalDate RETURNED = LocalDate.of(2026, 1, 20);

  private final NotificationRenderer renderer = new NotificationRenderer();

  private LendingEvent event(LendingEventType type, LocalDate returnedDate) {
    return new LendingEvent(type, READER, EMAIL, PHONE, LENDING, START, LIMIT, returnedDate);
  }

  @Test
  void createdRendersRegistrationAndDueDateToTheReaderBundle() {
    Notification n = renderer.render(event(LendingEventType.CREATED, null));

    assertThat(n.getSubject()).isEqualTo("New lending 2024/15");
    assertThat(n.getBody())
        .isEqualTo(
            "Dear reader 2024/1, your lending 2024/15 was registered on 2026-01-10 and is due on"
                + " 2026-01-25.");
    assertThat(n.getReaderNumber()).isEqualTo(READER);
    assertThat(n.getEmail()).isEqualTo(EMAIL);
    assertThat(n.getPhone()).isEqualTo(PHONE);
    assertThat(n.getEventType()).isEqualTo(LendingEventType.CREATED);
    assertThat(n.getLendingNumber()).isEqualTo(LENDING);
    assertThat(n.getLimitDate()).isEqualTo(LIMIT);
  }

  @Test
  void renewedRendersTheNewDueDate() {
    Notification n = renderer.render(event(LendingEventType.RENEWED, null));

    assertThat(n.getSubject()).isEqualTo("Lending 2024/15 renewed");
    assertThat(n.getBody())
        .isEqualTo(
            "Dear reader 2024/1, your lending 2024/15 was renewed and is now due on 2026-01-25.");
    assertThat(n.getEventType()).isEqualTo(LendingEventType.RENEWED);
  }

  @Test
  void returnedRendersTheReturnDate() {
    Notification n = renderer.render(event(LendingEventType.RETURNED, RETURNED));

    assertThat(n.getSubject()).isEqualTo("Lending 2024/15 returned");
    assertThat(n.getBody())
        .isEqualTo(
            "Dear reader 2024/1, your return of lending 2024/15 was registered on 2026-01-20.");
    assertThat(n.getEventType()).isEqualTo(LendingEventType.RETURNED);
  }

  @Test
  void overdueRendersTheMissedDueDateAndACallToAction() {
    Notification n = renderer.render(event(LendingEventType.OVERDUE, null));

    assertThat(n.getSubject()).isEqualTo("Lending 2024/15 overdue");
    assertThat(n.getBody())
        .isEqualTo(
            "Dear reader 2024/1, your lending 2024/15 was due on 2026-01-25 and is now overdue."
                + " Please return or renew it.");
    assertThat(n.getEventType()).isEqualTo(LendingEventType.OVERDUE);
  }

  @Test
  void carriesAMissingPhoneThroughAsNullSoAChannelCanSkipQuietly() {
    LendingEvent noPhone =
        new LendingEvent(
            LendingEventType.CREATED, READER, EMAIL, null, LENDING, START, LIMIT, null);

    Notification n = renderer.render(noPhone);

    assertThat(n.getPhone()).isNull();
    assertThat(n.getEmail()).isEqualTo(EMAIL);
  }
}
