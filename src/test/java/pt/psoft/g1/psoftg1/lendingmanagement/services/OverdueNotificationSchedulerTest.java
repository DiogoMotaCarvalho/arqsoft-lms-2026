package pt.psoft.g1.psoftg1.lendingmanagement.services;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEvent;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

/**
 * Tests the overdue sweep (issue #18, ADR-0014). It asserts the outcome, that the sweep publishes
 * exactly one overdue event for each overdue-and-not-yet-notified lending and marks each, and that
 * because of the marker a second run notifies nothing. The repository query is faked with its real
 * selection semantics (not returned, past the limit date, not yet notified) over plain {@link
 * Lending} objects, so marking a lending in the first run genuinely removes it from the second
 * run's selection. The publisher is recorded so the published events are asserted directly.
 */
class OverdueNotificationSchedulerTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 2, 1);

  private final List<Lending> all = new ArrayList<>();
  private final List<Object> published = new ArrayList<>();

  private Book book;
  private OverdueNotificationScheduler scheduler;

  @BeforeEach
  void setUp() {
    final List<Author> authors =
        List.of(new Author("Manuel Antonio Pina", "A Portuguese writer and journalist.", null));
    book =
        new Book(
            "9782826012092",
            "O Inspetor Max",
            "A police dog story.",
            new Genre("Romance"),
            authors,
            null);

    final LendingRepository repository = mock(LendingRepository.class);
    when(repository.listOverdueNotNotified(any()))
        .thenAnswer(
            invocation -> {
              final LocalDate today = invocation.getArgument(0);
              return all.stream()
                  .filter(
                      l ->
                          l.getReturnedDate() == null
                              && l.getLimitDate().isBefore(today)
                              && !l.isOverdueNotified())
                  .collect(toList());
            });

    final ApplicationEventPublisher publisher =
        new ApplicationEventPublisher() {
          @Override
          public void publishEvent(Object event) {
            published.add(event);
          }
        };

    scheduler = new OverdueNotificationScheduler(repository, publisher);
  }

  private Lending overdue(int readerNumber, int seq, LocalDate limitDate) {
    final ReaderDetails readerDetails =
        new ReaderDetails(
            readerNumber,
            Reader.newReader("reader" + readerNumber + "@gmail.com", "Manuelino123!", "A Reader"),
            "2000-01-01",
            "919191919",
            true,
            true,
            true,
            null,
            null);
    return new Lending(book, readerDetails, seq, limitDate, 200);
  }

  @Test
  void publishesOneOverdueEventPerOverdueLendingAndMarksEach() {
    final Lending first = overdue(1, 1, TODAY.minusDays(5));
    final Lending second = overdue(2, 2, TODAY.minusDays(1));
    final Lending notOverdue = overdue(3, 3, TODAY.plusDays(3)); // due in the future
    all.add(first);
    all.add(second);
    all.add(notOverdue);

    final int notified = scheduler.sweep(TODAY);

    assertThat(notified).isEqualTo(2);
    assertThat(published).hasSize(2);
    assertThat(published)
        .allSatisfy(
            e -> assertThat(((LendingEvent) e).getType()).isEqualTo(LendingEventType.OVERDUE));
    assertThat(published.stream().map(e -> ((LendingEvent) e).getLendingNumber()).collect(toList()))
        .containsExactlyInAnyOrder(first.getLendingNumber(), second.getLendingNumber());
    assertThat(first.isOverdueNotified()).isTrue();
    assertThat(second.isOverdueNotified()).isTrue();
    assertThat(notOverdue.isOverdueNotified()).isFalse();
  }

  @Test
  void aSecondRunNotifiesNothingBecauseTheMarkerExcludesAlreadyNotifiedLendings() {
    all.add(overdue(1, 1, TODAY.minusDays(5)));

    final int first = scheduler.sweep(TODAY);
    final int second = scheduler.sweep(TODAY);

    assertThat(first).isEqualTo(1);
    assertThat(second).isZero();
    assertThat(published).hasSize(1);
  }
}
