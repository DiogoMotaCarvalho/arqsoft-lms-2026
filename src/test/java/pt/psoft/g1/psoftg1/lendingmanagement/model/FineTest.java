package pt.psoft.g1.psoftg1.lendingmanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

class FineTest {
  private final Book book =
      new Book(
          "9782826012092",
          "O Inspetor Max",
          null,
          new Genre("Policial"),
          List.of(new Author("Manuel Antonio Pina", "Bio", null)),
          null);
  private final ReaderDetails readerDetails =
      new ReaderDetails(
          1,
          Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Coives"),
          "2000-01-01",
          "919191919",
          true,
          false,
          false,
          null,
          null);

  private Lending lending(int daysAgo, LocalDate returned, int finePerDay) {
    return Lending.newBootstrappingLending(
        book, readerDetails, 2024, 1, LocalDate.now().minusDays(daysAgo), returned, 10, finePerDay);
  }

  @Test
  void ensureFineCannotBeCreatedForLendingThatIsNotOverdue() {
    assertThrows(IllegalArgumentException.class, () -> new Fine(lending(5, null, 200)));
  }

  @Test
  void ensureFineCannotBeCreatedForLendingReturnedOnTime() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Fine(lending(30, LocalDate.now().minusDays(25), 200)));
  }

  @Test
  void ensureFineCannotBeCreatedOnTheLimitDate() {
    assertThrows(IllegalArgumentException.class, () -> new Fine(lending(10, null, 200)));
  }

  @Test
  void ensureFineCannotBeCreatedForLendingReturnedOnTheLimitDate() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Fine(lending(30, LocalDate.now().minusDays(20), 200)));
  }

  @Test
  void ensureFineStartsTheDayAfterTheLimitDate() {
    assertEquals(200, new Fine(lending(11, null, 200)).getCentsValue());
  }

  @Test
  void ensureFineValueIsDaysDelayedTimesValuePerDay() {
    Lending overdue = lending(13, null, 150);

    Fine fine = new Fine(overdue);

    assertEquals(150, fine.getFineValuePerDayInCents());
    assertEquals(3 * 150, fine.getCentsValue());
    assertSame(overdue, fine.getLending());
  }

  @Test
  void ensureFineUsesReturnedDateForReturnedLateLending() {
    Fine fine = new Fine(lending(30, LocalDate.now().minusDays(15), 100));

    assertEquals(5 * 100, fine.getCentsValue());
  }
}
