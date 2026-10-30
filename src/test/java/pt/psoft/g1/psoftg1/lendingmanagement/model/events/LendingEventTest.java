package pt.psoft.g1.psoftg1.lendingmanagement.model.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

/**
 * Transparent-box test of the event snapshot's resolution (issue #18, ADR-0014). It asserts that
 * {@link LendingEvent#of} resolves the recipient contacts at publish time from the reader details,
 * the email being the account username and the phone the reader's phone number, and copies the
 * lending identity and dates, so the notification context never touches the live entity. The
 * lending is mocked to keep the test a plain unit, focused on the mapping rather than entity
 * construction.
 */
@ExtendWith(MockitoExtension.class)
class LendingEventTest {

  @Mock private Lending lending;
  @Mock private ReaderDetails readerDetails;
  @Mock private Reader reader;

  @Test
  void ofResolvesRecipientContactsAndCopiesTheLendingSnapshot() {
    final LocalDate start = LocalDate.of(2026, 1, 10);
    final LocalDate limit = LocalDate.of(2026, 1, 25);
    final LocalDate returned = LocalDate.of(2026, 1, 20);

    when(lending.getReaderDetails()).thenReturn(readerDetails);
    when(readerDetails.getReader()).thenReturn(reader);
    when(reader.getUsername()).thenReturn("reader@example.com");
    when(readerDetails.getPhoneNumber()).thenReturn("912345678");
    when(lending.getReaderNumber()).thenReturn("2024/1");
    when(lending.getLendingNumber()).thenReturn("2024/15");
    when(lending.getStartDate()).thenReturn(start);
    when(lending.getLimitDate()).thenReturn(limit);
    when(lending.getReturnedDate()).thenReturn(returned);

    LendingEvent event = LendingEvent.of(LendingEventType.RETURNED, lending);

    assertThat(event.getType()).isEqualTo(LendingEventType.RETURNED);
    assertThat(event.getReaderNumber()).isEqualTo("2024/1");
    assertThat(event.getEmail()).isEqualTo("reader@example.com");
    assertThat(event.getPhone()).isEqualTo("912345678");
    assertThat(event.getLendingNumber()).isEqualTo("2024/15");
    assertThat(event.getStartDate()).isEqualTo(start);
    assertThat(event.getLimitDate()).isEqualTo(limit);
    assertThat(event.getReturnedDate()).isEqualTo(returned);
  }
}
