package pt.psoft.g1.psoftg1.lendingmanagement.repositories;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.shared.services.Page;

public class InMemoryLendingRepository implements LendingRepository {
  private final List<Lending> lendings = new ArrayList<>();

  @Override
  public Lending save(Lending lending) {
    if (!lendings.contains(lending)) {
      lendings.add(lending);
    }
    return lending;
  }

  @Override
  public Optional<Lending> findByLendingNumber(String lendingNumber) {
    return lendings.stream().filter(l -> l.getLendingNumber().equals(lendingNumber)).findFirst();
  }

  @Override
  public List<Lending> listByReaderNumberAndIsbn(String readerNumber, String isbn) {
    return new ArrayList<>(
        lendings.stream()
            .filter(l -> l.getReaderDetails().getReaderNumber().equals(readerNumber))
            .filter(l -> l.getBook().getIsbn().equals(isbn))
            .toList());
  }

  @Override
  public List<Lending> listOutstandingByReaderNumber(String readerNumber) {
    return lendings.stream()
        .filter(l -> l.getReaderDetails().getReaderNumber().equals(readerNumber))
        .filter(l -> l.getReturnedDate() == null)
        .toList();
  }

  @Override
  public List<Lending> listOverdueNotNotified(LocalDate today) {
    return lendings.stream()
        .filter(l -> l.getReturnedDate() == null)
        .filter(l -> l.getLimitDate().isBefore(today))
        .filter(l -> !l.isOverdueNotified())
        .toList();
  }

  @Override
  public List<Lending> listCompletedByReaderNumberSince(String readerNumber, LocalDate since) {
    return lendings.stream()
        .filter(l -> l.getReaderDetails().getReaderNumber().equals(readerNumber))
        .filter(l -> l.getReturnedDate() != null)
        .filter(l -> !l.getReturnedDate().isBefore(since))
        .toList();
  }

  @Override
  public int getCountFromCurrentYear() {
    int currentYear = LocalDate.now().getYear();
    return (int) lendings.stream().filter(l -> l.getStartDate().getYear() == currentYear).count();
  }

  public List<Lending> findAll() {
    return List.copyOf(lendings);
  }

  @Override
  public Double getAverageDuration() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Double getAvgLendingDurationByIsbn(String isbn) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Lending> getOverdue(Page page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Lending> searchLendings(
      Page page,
      String readerNumber,
      String isbn,
      Boolean returned,
      LocalDate startDate,
      LocalDate endDate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(Lending lending) {
    lendings.remove(lending);
  }
}
