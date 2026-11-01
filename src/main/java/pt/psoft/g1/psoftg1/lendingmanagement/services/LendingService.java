package pt.psoft.g1.psoftg1.lendingmanagement.services;

import java.util.List;
import java.util.Optional;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.shared.services.Page;

public interface LendingService {
  /**
   * Finds a lending by its lending number.
   *
   * @param lendingNumber the lending number to search for
   * @return {@code Optional<Lending>}
   */
  Optional<Lending> findByLendingNumber(String lendingNumber);

  /**
   * Searches lendings matching the given reader, book and return filters.
   *
   * @param readerNumber - Reader Number of the Reader associated with the lending
   * @param isbn - ISBN of the book associated with the lending
   * @param returned - Wether it's intended to filter by the return status of a lending
   * @return {@code Iterable<Lending>}
   */
  List<Lending> listByReaderNumberAndIsbn(
      String readerNumber, String isbn, Optional<Boolean> returned);

  Lending create(CreateLendingRequest resource); // No ID passed, as it is auto generated

  Lending setReturned(String id, SetLendingReturnedRequest resource, long desiredVersion);

  /**
   * Renews an existing lending. The applicable lending policy is re-evaluated at this moment (per
   * the Lending Policies Specification), decides whether the renewal is allowed and what the new
   * limit date is, and the lending records the extension. See ADR-0007.
   *
   * @param lendingNumber the lending to renew
   * @param desiredVersion the client's expected version, for optimistic locking
   * @return the renewed lending
   */
  Lending renew(String lendingNumber, long desiredVersion);

  Double getAverageDuration();

  List<Lending> getOverdue(Page page);

  Double getAvgLendingDurationByIsbn(String isbn);

  List<Lending> searchLendings(Page page, SearchLendingQuery request);
}
