package pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.impl;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;

public interface SpringDataLendingRepository
    extends LendingRepository, LendingRepoCustom, CrudRepository<Lending, Long> {
  @Override
  @Query("SELECT l " + "FROM Lending l " + "WHERE l.lendingNumber.lendingNumber = :lendingNumber")
  Optional<Lending> findByLendingNumber(String lendingNumber);

  // http://www.h2database.com/html/commands.html

  @Override
  @Query(
      "SELECT l "
          + "FROM Lending l "
          + "JOIN Book b ON l.book.pk = b.pk "
          + "JOIN ReaderDetails r ON l.readerDetails.pk = r.pk "
          + "WHERE b.isbn.isbn = :isbn "
          + "AND r.readerNumber.readerNumber = :readerNumber ")
  List<Lending> listByReaderNumberAndIsbn(String readerNumber, String isbn);

  @Override
  @Query("SELECT COUNT (l) " + "FROM Lending l " + "WHERE YEAR(l.startDate) = YEAR(CURRENT_DATE)")
  int getCountFromCurrentYear();

  @Override
  @Query(
      "SELECT l "
          + "FROM Lending l "
          + "JOIN ReaderDetails r ON l.readerDetails.pk = r.pk "
          + "WHERE r.readerNumber.readerNumber = :readerNumber "
          + "AND l.returnedDate IS NULL")
  List<Lending> listOutstandingByReaderNumber(@Param("readerNumber") String readerNumber);

  @Override
  @Query(
      value = "SELECT AVG(DATEDIFF(day, l.start_date, l.returned_date)) " + "FROM Lending l",
      nativeQuery = true)
  Double getAverageDuration();

  @Override
  @Query(
      value =
          "SELECT AVG(DATEDIFF(day, l.start_date, l.returned_date)) "
              + "FROM Lending l "
              + "JOIN BOOK b ON l.BOOK_PK = b.PK "
              + "WHERE b.ISBN = :isbn",
      nativeQuery = true)
  Double getAvgLendingDurationByIsbn(@Param("isbn") String isbn);
}
