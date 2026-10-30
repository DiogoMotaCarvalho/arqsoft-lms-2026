package pt.psoft.g1.psoftg1.lendingmanagement.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.shared.services.Page;

public interface LendingRepository {
  Optional<Lending> findByLendingNumber(String lendingNumber);

  List<Lending> listByReaderNumberAndIsbn(String readerNumber, String isbn);

  int getCountFromCurrentYear();

  List<Lending> listOutstandingByReaderNumber(String readerNumber);

  /** Completed (returned) lendings of a reader whose return date is on or after {@code since}. */
  List<Lending> listCompletedByReaderNumberSince(String readerNumber, LocalDate since);

  Double getAverageDuration();

  Double getAvgLendingDurationByIsbn(String isbn);

  List<Lending> getOverdue(Page page);

  /**
   * Lendings that are now overdue (not returned and past their limit date) and whose
   * becoming-overdue has not yet been notified, as of {@code today}. Used by the overdue sweep
   * (issue #18) so a lending is notified at most once.
   */
  List<Lending> listOverdueNotNotified(LocalDate today);

  List<Lending> searchLendings(
      Page page,
      String readerNumber,
      String isbn,
      Boolean returned,
      LocalDate startDate,
      LocalDate endDate);

  Lending save(Lending lending);

  void delete(Lending lending);
}
