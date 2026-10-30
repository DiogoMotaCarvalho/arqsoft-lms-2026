package pt.psoft.g1.psoftg1.lendingmanagement.services;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.InMemoryBookRepository;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.InMemoryFineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.InMemoryLendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.InMemoryReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

class LendingServiceImplTest {
  private static final int LENDING_DURATION = LendingPolicyFixtures.STANDARD_DURATION;
  private static final int FINE_PER_DAY = 200;
  private static final LocalDate TODAY = LocalDate.now();
  private static final String ISBN = "9782826012092";

  private final InMemoryLendingRepository lendingRepository = new InMemoryLendingRepository();
  private final InMemoryFineRepository fineRepository = new InMemoryFineRepository();
  private final InMemoryBookRepository bookRepository = new InMemoryBookRepository();
  private final InMemoryReaderRepository readerRepository = new InMemoryReaderRepository();

  private final LendingServiceImpl service =
      new LendingServiceImpl(
          lendingRepository,
          fineRepository,
          bookRepository,
          readerRepository,
          new SelectingLendingPolicyProvider(lendingRepository, LendingPolicyFixtures.factory()));

  private Book book;
  private ReaderDetails reader;
  private String readerNumber;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "fineValuePerDayInCents", FINE_PER_DAY);

    book =
        bookRepository.save(
            new Book(
                ISBN,
                "O Inspetor Max",
                null,
                new Genre("Policial"),
                List.of(new Author("Manuel Antonio Pina", "Escritor português", null)),
                null));
    reader =
        readerRepository.save(
            new ReaderDetails(
                1,
                Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Coives"),
                "2000-01-01",
                "919191919",
                true,
                false,
                false,
                null,
                null));
    readerNumber = reader.getReaderNumber();
  }

  private Lending existingLending(int seq, int startedDaysAgo, LocalDate returnedDate) {
    return lendingRepository.save(
        Lending.newBootstrappingLending(
            book,
            reader,
            TODAY.getYear(),
            seq,
            TODAY.minusDays(startedDaysAgo),
            returnedDate,
            LENDING_DURATION,
            FINE_PER_DAY));
  }

  private Lending lend() {
    return service.create(new CreateLendingRequest(ISBN, readerNumber));
  }

  @Test
  void ensureReaderCanBorrowABook() {
    Lending lending = lend();

    assertSame(book, lending.getBook());
    assertSame(reader, lending.getReaderDetails());
    assertEquals(TODAY.plusDays(LENDING_DURATION), lending.getLimitDate());
    assertEquals(FINE_PER_DAY, lending.getFineValuePerDayInCents());
    assertEquals(List.of(lending), lendingRepository.findAll());
  }

  @Test
  void ensureLendingNumberFollowsTheYearlySequence() {
    existingLending(1, 2, TODAY);
    existingLending(2, 1, TODAY);

    assertEquals(TODAY.getYear() + "/3", lend().getLendingNumber());
  }

  @Test
  void ensureReaderWithTwoOutstandingBooksCanBorrowAThird() {
    existingLending(1, 1, null);
    existingLending(2, 1, null);

    assertDoesNotThrow(this::lend);
  }

  @Test
  void ensureReaderCannotHaveMoreThanThreeOutstandingBooks() {
    existingLending(1, 1, null);
    existingLending(2, 1, null);
    existingLending(3, 1, null);

    assertThrows(LendingForbiddenException.class, this::lend);
    assertEquals(3, lendingRepository.findAll().size());
  }

  @Test
  void ensureReturnedBooksDoNotCountAsOutstanding() {
    existingLending(1, 1, TODAY);
    existingLending(2, 1, TODAY);
    existingLending(3, 1, TODAY);

    assertDoesNotThrow(this::lend);
  }

  @Test
  void ensureReaderWithOverdueBookCannotBorrow() {
    existingLending(1, LENDING_DURATION + 1, null);

    assertThrows(LendingForbiddenException.class, this::lend);
  }

  @Test
  void ensureBookReturnedLateInThePastDoesNotBlockNewLendings() {
    existingLending(1, 40, TODAY.minusDays(10));

    assertDoesNotThrow(this::lend);
  }

  @Test
  void ensureCannotBorrowUnknownBook() {
    CreateLendingRequest request = new CreateLendingRequest("8175257660", readerNumber);

    assertThrows(NotFoundException.class, () -> service.create(request));
  }

  @Test
  void ensureUnknownReaderCannotBorrow() {
    CreateLendingRequest request = new CreateLendingRequest(ISBN, "2024/999");

    assertThrows(NotFoundException.class, () -> service.create(request));
  }

  @Test
  void ensureReturningOnTimeDoesNotCreateFine() {
    Lending lending = existingLending(1, 5, null);

    service.setReturned(lending.getLendingNumber(), new SetLendingReturnedRequest("Bom"), 0);

    assertEquals(TODAY, lending.getReturnedDate());
    assertFalse(fineRepository.findAll().iterator().hasNext());
  }

  @Test
  void ensureReturningLateCreatesFineForEachDayOfDelay() {
    Lending lending = existingLending(1, LENDING_DURATION + 4, null);

    service.setReturned(lending.getLendingNumber(), new SetLendingReturnedRequest(null), 0);

    Optional<Fine> fine = fineRepository.findByLendingNumber(lending.getLendingNumber());
    assertTrue(fine.isPresent());
    assertEquals(4 * FINE_PER_DAY, fine.get().getCentsValue());
  }

  @Test
  void ensureCannotReturnUnknownLending() {
    SetLendingReturnedRequest request = new SetLendingReturnedRequest(null);

    assertThrows(NotFoundException.class, () -> service.setReturned("2024/999", request, 0));
  }

  @Test
  void ensureCannotReturnWithStaleVersion() {
    Lending lending = existingLending(1, 1, null);
    SetLendingReturnedRequest request = new SetLendingReturnedRequest(null);

    assertThrows(
        StaleObjectStateException.class,
        () -> service.setReturned(lending.getLendingNumber(), request, 7));
    assertNull(lending.getReturnedDate());
  }

  @Test
  void ensureListingCanBeFilteredByReturnedState() {
    Lending returned = existingLending(1, 5, TODAY);
    Lending outstanding = existingLending(2, 1, null);

    assertEquals(
        List.of(returned),
        service.listByReaderNumberAndIsbn(readerNumber, ISBN, Optional.of(true)));
    assertEquals(
        List.of(outstanding),
        service.listByReaderNumberAndIsbn(readerNumber, ISBN, Optional.of(false)));
    assertEquals(
        List.of(returned, outstanding),
        service.listByReaderNumberAndIsbn(readerNumber, ISBN, Optional.empty()));
  }

  @Test
  void ensureSearchRejectsInvalidDates() {
    SearchLendingQuery query = new SearchLendingQuery("", "", null, "01-01-2024", null);

    assertThrows(IllegalArgumentException.class, () -> service.searchLendings(null, query));
  }
}
