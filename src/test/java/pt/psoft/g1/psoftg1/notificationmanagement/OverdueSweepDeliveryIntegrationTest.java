package pt.psoft.g1.psoftg1.notificationmanagement;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.OverdueNotificationScheduler;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

/**
 * End-to-end test that the overdue sweep actually delivers (issue #18, ADR-0014). It is the
 * regression guard for the sweep being transactional: the sweep publishes overdue events through
 * the same after-commit listener as the lending flow, so it must run inside a transaction for the
 * events to be delivered rather than discarded. A committed overdue lending is swept through the
 * real scheduler bean and the test asserts both the delivered OVERDUE notification on the recording
 * channel and the persisted once-only marker. Not transactional at the class level, so the sweep's
 * own transaction truly commits.
 */
@SpringBootTest
@Import(NotificationTriggerIntegrationTest.RecordingChannelConfig.class)
class OverdueSweepDeliveryIntegrationTest {

  private static final String ISBN = "9782826012092";

  @Autowired private OverdueNotificationScheduler scheduler;
  @Autowired private LendingRepository lendingRepository;
  @Autowired private ReaderRepository readerRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private BookRepository bookRepository;
  @Autowired private GenreRepository genreRepository;
  @Autowired private AuthorRepository authorRepository;
  @Autowired private NotificationTriggerIntegrationTest.RecordingChannel recordingChannel;

  private Author author;
  private Genre genre;
  private Book book;
  private Reader reader;
  private ReaderDetails readerDetails;
  private Lending overdueLending;
  private String lendingNumber;

  @BeforeEach
  void setUp() {
    recordingChannel.delivered.clear();

    author = authorRepository.save(new Author("Sweep Author", "A writer.", null));
    genre = genreRepository.save(new Genre("Sweep"));
    book =
        bookRepository.save(
            new Book(ISBN, "O Inspetor Max", "A police dog story.", genre, List.of(author), null));
    reader = userRepository.save(Reader.newReader("sweep@gmail.com", "Manuelino123!", "A Reader"));
    readerDetails =
        readerRepository.save(
            new ReaderDetails(
                880, reader, "2000-01-01", "919191919", true, true, true, null, null));

    final int year = LocalDate.now().getYear();
    overdueLending =
        lendingRepository.save(
            Lending.newBootstrappingLending(
                book,
                readerDetails,
                year,
                555,
                LocalDate.now().minusDays(30), // started well in the past
                null, // not returned
                15, // due 15 days after start, so overdue now
                200));
    lendingNumber = overdueLending.getLendingNumber();
  }

  @AfterEach
  void tearDown() {
    // Re-fetch: the sweep updates the lending in its own transaction, so the setUp reference is
    // stale.
    lendingRepository.findByLendingNumber(lendingNumber).ifPresent(lendingRepository::delete);
    readerRepository.delete(readerDetails);
    userRepository.delete(reader);
    bookRepository.delete(book);
    genreRepository.delete(genre);
    authorRepository.delete(author);
  }

  @Test
  void theSweepDeliversAnOverdueNotificationAndMarksTheLending() {
    scheduler.checkOverdue();

    await()
        .atMost(5, SECONDS)
        .untilAsserted(
            () ->
                assertThat(recordingChannel.delivered)
                    .anySatisfy(
                        d -> {
                          assertThat(d.notification().getEventType())
                              .isEqualTo(LendingEventType.OVERDUE);
                          assertThat(d.notification().getLendingNumber()).isEqualTo(lendingNumber);
                          assertThat(d.notification().getEmail()).isEqualTo("sweep@gmail.com");
                        }));

    assertThat(lendingRepository.findByLendingNumber(lendingNumber))
        .get()
        .satisfies(l -> assertThat(l.isOverdueNotified()).isTrue());
  }
}
