package pt.psoft.g1.psoftg1.lendingmanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

class LendingTest {
  private static final int LENDING_DURATION = 15;
  private static final int FINE_PER_DAY = 200;
  private static final LocalDate TODAY = LocalDate.now();

  private final Book book =
      new Book(
          "9782826012092",
          "O Inspetor Max",
          null,
          new Genre("Romance"),
          List.of(new Author("Manuel Antonio Pina", "Jornalista e escritor português", null)),
          null);
  private final ReaderDetails readerDetails =
      new ReaderDetails(
          1,
          Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Sarapinto das Coives"),
          "2000-01-01",
          "919191919",
          true,
          true,
          true,
          null,
          null);

  private Lending newLending() {
    return new Lending(book, readerDetails, 1, TODAY.plusDays(LENDING_DURATION), FINE_PER_DAY);
  }

  private Lending startedDaysAgo(int daysAgo, LocalDate returnedDate) {
    return Lending.newBootstrappingLending(
        book,
        readerDetails,
        2024,
        5,
        TODAY.minusDays(daysAgo),
        returnedDate,
        LENDING_DURATION,
        FINE_PER_DAY);
  }

  @Test
  void ensureBookNotNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Lending(null, readerDetails, 1, TODAY.plusDays(LENDING_DURATION), FINE_PER_DAY));
  }

  @Test
  void ensureReaderNotNull() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Lending(book, null, 1, TODAY.plusDays(LENDING_DURATION), FINE_PER_DAY));
  }

  @Test
  void ensureSequentialNumberCannotBeNegative() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Lending(book, readerDetails, -1, TODAY.plusDays(LENDING_DURATION), FINE_PER_DAY));
  }

  @Test
  void ensureLimitDateIsRequired() {
    assertThrows(
        NullPointerException.class, () -> new Lending(book, readerDetails, 1, null, FINE_PER_DAY));
  }

  @Test
  void ensureNewLendingStartsTodayAndIsDueOnGivenLimitDate() {
    Lending lending = newLending();

    assertEquals(TODAY, lending.getStartDate());
    assertEquals(TODAY.plusDays(LENDING_DURATION), lending.getLimitDate());
    assertEquals(Optional.of(LENDING_DURATION), lending.getDaysUntilReturn());
  }

  @Test
  void ensureNewLendingIsNotReturnedNorOverdue() {
    Lending lending = newLending();

    assertNull(lending.getReturnedDate());
    assertEquals(0, lending.getDaysDelayed());
    assertEquals(Optional.empty(), lending.getDaysOverdue());
    assertEquals(Optional.empty(), lending.getFineValueInCents());
  }

  @Test
  void ensureNewLendingKeepsBookReaderAndFineValue() {
    Lending lending = newLending();

    assertSame(book, lending.getBook());
    assertSame(readerDetails, lending.getReaderDetails());
    assertEquals("O Inspetor Max", lending.getTitle());
    assertEquals(FINE_PER_DAY, lending.getFineValuePerDayInCents());
  }

  @Test
  void ensureNewLendingNumberUsesCurrentYearAndSequential() {
    assertEquals(TODAY.getYear() + "/1", newLending().getLendingNumber());
  }

  @Test
  void ensureLendingDueTodayIsNotOverdue() {
    Lending lending = new Lending(book, readerDetails, 1, TODAY, FINE_PER_DAY);

    assertEquals(TODAY, lending.getLimitDate());
    assertEquals(Optional.of(0), lending.getDaysUntilReturn());
    assertEquals(0, lending.getDaysDelayed());
  }

  @Test
  void ensureReturningSetsReturnedDateToToday() {
    Lending lending = newLending();

    lending.setReturned(0, "Gostei muito");

    assertEquals(TODAY, lending.getReturnedDate());
    assertEquals(Optional.empty(), lending.getDaysUntilReturn());
  }

  @Test
  void ensureCommentaryIsOptionalWhenReturning() {
    Lending lending = newLending();

    lending.setReturned(0, null);

    assertEquals(TODAY, lending.getReturnedDate());
  }

  @Test
  void ensureCannotReturnTwice() {
    Lending lending = startedDaysAgo(1, null);
    lending.setReturned(0, null);

    assertThrows(IllegalArgumentException.class, () -> lending.setReturned(0, null));
  }

  @Test
  void ensureCannotReturnWithStaleVersion() {
    Lending lending = startedDaysAgo(1, null);

    assertThrows(StaleObjectStateException.class, () -> lending.setReturned(1, "comment"));
    assertNull(lending.getReturnedDate());
  }

  @Test
  void ensureBootstrappingLendingUsesGivenValues() {
    Lending lending = startedDaysAgo(3, null);

    assertEquals("2024/5", lending.getLendingNumber());
    assertEquals(TODAY.minusDays(3), lending.getStartDate());
    assertEquals(TODAY.plusDays(12), lending.getLimitDate());
    assertEquals(FINE_PER_DAY, lending.getFineValuePerDayInCents());
  }

  @Test
  void ensureBootstrappingLendingRejectsNullBookOrReader() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Lending.newBootstrappingLending(null, readerDetails, 2024, 1, TODAY, null, 15, 200));
    assertThrows(
        IllegalArgumentException.class,
        () -> Lending.newBootstrappingLending(book, null, 2024, 1, TODAY, null, 15, 200));
  }

  @Test
  void ensureLendingWithinLimitHasNoFine() {
    Lending lending = startedDaysAgo(10, null);

    assertEquals(0, lending.getDaysDelayed());
    assertEquals(Optional.of(5), lending.getDaysUntilReturn());
    assertEquals(Optional.empty(), lending.getDaysOverdue());
    assertEquals(Optional.empty(), lending.getFineValueInCents());
  }

  @Test
  void ensureLendingIsNotOverdueOnItsLimitDate() {
    Lending lending = startedDaysAgo(LENDING_DURATION, null);

    assertEquals(Optional.of(0), lending.getDaysUntilReturn());
    assertEquals(0, lending.getDaysDelayed());
    assertEquals(Optional.empty(), lending.getFineValueInCents());
  }

  @Test
  void ensureLendingIsOverdueTheDayAfterItsLimitDate() {
    Lending lending = startedDaysAgo(LENDING_DURATION + 1, null);

    assertEquals(1, lending.getDaysDelayed());
    assertEquals(Optional.of(1), lending.getDaysOverdue());
    assertEquals(Optional.of(FINE_PER_DAY), lending.getFineValueInCents());
  }

  @Test
  void ensureOverdueLendingReportsDelayAndFine() {
    Lending lending = startedDaysAgo(20, null);

    assertEquals(5, lending.getDaysDelayed());
    assertEquals(Optional.of(5), lending.getDaysOverdue());
    assertEquals(Optional.empty(), lending.getDaysUntilReturn());
    assertEquals(Optional.of(5 * FINE_PER_DAY), lending.getFineValueInCents());
  }

  @Test
  void ensureReturningOverdueLendingKeepsDelayAndFine() {
    Lending lending = startedDaysAgo(20, null);

    lending.setReturned(0, null);

    assertEquals(5, lending.getDaysDelayed());
    assertEquals(Optional.of(5 * FINE_PER_DAY), lending.getFineValueInCents());
  }

  @Test
  void ensureDelayIsComputedUntilReturnedDate() {
    Lending lending = startedDaysAgo(30, TODAY.minusDays(12));

    assertEquals(3, lending.getDaysDelayed());
    assertEquals(Optional.of(3 * FINE_PER_DAY), lending.getFineValueInCents());
    assertEquals(Optional.empty(), lending.getDaysUntilReturn());
  }

  @Test
  void ensureReturnedOnTimeHasNoDelay() {
    Lending lending = startedDaysAgo(30, TODAY.minusDays(20));

    assertEquals(0, lending.getDaysDelayed());
    assertEquals(Optional.empty(), lending.getDaysOverdue());
    assertEquals(Optional.empty(), lending.getFineValueInCents());
  }

  @Test
  void ensureNewLendingHasNotBeenRenewed() {
    assertEquals(0, newLending().getRenewalCount());
  }

  @Test
  void ensureRenewingMovesLimitDateAndCountsTheRenewal() {
    Lending lending = newLending();

    lending.renew(0, TODAY.plusDays(30));

    assertEquals(TODAY.plusDays(30), lending.getLimitDate());
    assertEquals(Optional.of(30), lending.getDaysUntilReturn());
    assertEquals(1, lending.getRenewalCount());
  }

  @Test
  void ensureEveryRenewalIsCounted() {
    Lending lending = newLending();

    lending.renew(0, TODAY.plusDays(20));
    lending.renew(0, TODAY.plusDays(25));

    assertEquals(TODAY.plusDays(25), lending.getLimitDate());
    assertEquals(2, lending.getRenewalCount());
  }

  @Test
  void ensureRenewingOverdueLendingRecomputesOverdueState() {
    Lending lending = startedDaysAgo(LENDING_DURATION + 3, null);

    lending.renew(0, TODAY.plusDays(5));

    assertEquals(Optional.empty(), lending.getDaysOverdue());
    assertEquals(Optional.empty(), lending.getFineValueInCents());
    assertEquals(Optional.of(5), lending.getDaysUntilReturn());
  }

  @Test
  void ensureReturnedLendingCannotBeRenewed() {
    Lending lending = startedDaysAgo(1, null);
    lending.setReturned(0, null);
    LocalDate limitDate = lending.getLimitDate();

    assertThrows(IllegalArgumentException.class, () -> lending.renew(0, TODAY.plusDays(30)));
    assertEquals(limitDate, lending.getLimitDate());
    assertEquals(0, lending.getRenewalCount());
  }

  @Test
  void ensureCannotRenewWithStaleVersion() {
    Lending lending = newLending();

    assertThrows(StaleObjectStateException.class, () -> lending.renew(1, TODAY.plusDays(30)));
    assertEquals(TODAY.plusDays(LENDING_DURATION), lending.getLimitDate());
    assertEquals(0, lending.getRenewalCount());
  }

  @Test
  void ensureRenewalRequiresNewLimitDate() {
    Lending lending = newLending();

    assertThrows(NullPointerException.class, () -> lending.renew(0, null));
    assertEquals(TODAY.plusDays(LENDING_DURATION), lending.getLimitDate());
    assertEquals(0, lending.getRenewalCount());
  }
}
