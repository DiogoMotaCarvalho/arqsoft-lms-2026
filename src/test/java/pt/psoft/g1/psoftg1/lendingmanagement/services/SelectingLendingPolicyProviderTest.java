package pt.psoft.g1.psoftg1.lendingmanagement.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.InMemoryLendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

class SelectingLendingPolicyProviderTest {
  private static final LocalDate TODAY = LocalDate.now();
  private static final LocalDate HISTORY_START =
      TODAY.minusMonths(LendingPolicyFixtures.HISTORY_MONTHS);
  private static final int DURATION = LendingPolicyFixtures.STANDARD_DURATION;

  private final InMemoryLendingRepository lendingRepository = new InMemoryLendingRepository();
  private final SelectingLendingPolicyProvider provider =
      new SelectingLendingPolicyProvider(lendingRepository, LendingPolicyFixtures.factory());

  private final Book book =
      new Book(
          "9782826012092",
          "O Inspetor Max",
          null,
          new Genre("Policial"),
          List.of(new Author("Manuel Antonio Pina", "Escritor português", null)),
          null);
  private final ReaderDetails reader = reader(1, "manuel@gmail.com");
  private int sequence;

  private static ReaderDetails reader(int number, String username) {
    return new ReaderDetails(
        number,
        Reader.newReader(username, "Manuelino123!", "Manuel Coives"),
        "2000-01-01",
        "919191919",
        true,
        false,
        false,
        null,
        null);
  }

  private void returnedOnTime(ReaderDetails owner, LocalDate returnedDate) {
    save(owner, returnedDate.minusDays(DURATION - 1), returnedDate);
  }

  private void returnedLate(ReaderDetails owner, LocalDate returnedDate) {
    save(owner, returnedDate.minusDays(DURATION + 5), returnedDate);
  }

  private void save(ReaderDetails owner, LocalDate startDate, LocalDate returnedDate) {
    lendingRepository.save(
        Lending.newBootstrappingLending(
            book, owner, TODAY.getYear(), ++sequence, startDate, returnedDate, DURATION, 200));
  }

  private void recentOnTimeReturns(ReaderDetails owner, int count) {
    for (int i = 0; i < count; i++) {
      returnedOnTime(owner, TODAY.minusDays(10 + i));
    }
  }

  private String policyName() {
    return provider.policyFor(reader).name();
  }

  @Test
  void ensureReaderWithGoodRecentHistoryGetsAdaptivePolicy() {
    recentOnTimeReturns(reader, LendingPolicyFixtures.MIN_COMPLETED);

    assertEquals("adaptive", policyName());
  }

  @Test
  void ensureReaderWithTooFewCompletedLendingsGetsStandardPolicy() {
    recentOnTimeReturns(reader, LendingPolicyFixtures.MIN_COMPLETED - 1);

    assertEquals("standard", policyName());
  }

  @Test
  void ensureReaderWithTooManyLateReturnsGetsStandardPolicy() {
    recentOnTimeReturns(reader, 3);
    returnedLate(reader, TODAY.minusDays(20));
    returnedLate(reader, TODAY.minusDays(30));

    assertEquals("standard", policyName());
  }

  @Test
  void ensureReaderWithAnOverdueLendingGetsStandardPolicy() {
    recentOnTimeReturns(reader, LendingPolicyFixtures.MIN_COMPLETED);
    save(reader, TODAY.minusDays(DURATION + 3), null);

    assertEquals("standard", policyName());
  }

  @Test
  void ensureOutstandingLendingWithinLimitDoesNotBlockAdaptivePolicy() {
    recentOnTimeReturns(reader, LendingPolicyFixtures.MIN_COMPLETED);
    save(reader, TODAY.minusDays(2), null);

    assertEquals("adaptive", policyName());
  }

  @Test
  void ensureLendingReturnedAtStartOfHistoryPeriodCounts() {
    recentOnTimeReturns(reader, LendingPolicyFixtures.MIN_COMPLETED - 1);
    returnedOnTime(reader, HISTORY_START);

    assertEquals("adaptive", policyName());
  }

  @Test
  void ensureLendingReturnedBeforeHistoryPeriodIsIgnored() {
    recentOnTimeReturns(reader, LendingPolicyFixtures.MIN_COMPLETED - 1);
    returnedOnTime(reader, HISTORY_START.minusDays(1));

    assertEquals("standard", policyName());
  }

  @Test
  void ensureOtherReadersHistoryIsIgnored() {
    recentOnTimeReturns(reader(2, "joao@gmail.com"), LendingPolicyFixtures.MIN_COMPLETED);

    assertEquals("standard", policyName());
  }

  @Test
  void ensureMissingReaderGetsStandardPolicy() {
    assertEquals("standard", provider.policyFor(null).name());
  }
}
