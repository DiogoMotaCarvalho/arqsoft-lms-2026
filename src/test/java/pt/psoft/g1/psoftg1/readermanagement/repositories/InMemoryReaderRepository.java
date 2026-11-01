package pt.psoft.g1.psoftg1.readermanagement.repositories;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderBookCountDto;
import pt.psoft.g1.psoftg1.readermanagement.services.SearchReadersQuery;

public class InMemoryReaderRepository implements ReaderRepository {
  private final List<ReaderDetails> readers = new ArrayList<>();

  @Override
  public ReaderDetails save(ReaderDetails readerDetails) {
    if (readerDetails.getVersion() == null) {
      ReflectionTestUtils.setField(readerDetails, "version", 0L);
    }
    if (!readers.contains(readerDetails)) {
      readers.add(readerDetails);
    }
    return readerDetails;
  }

  @Override
  public Optional<ReaderDetails> findByReaderNumber(String readerNumber) {
    return readers.stream().filter(r -> r.getReaderNumber().equals(readerNumber)).findFirst();
  }

  @Override
  public Optional<ReaderDetails> findByUsername(String username) {
    return readers.stream().filter(r -> r.getReader().getUsername().equals(username)).findFirst();
  }

  @Override
  public Optional<ReaderDetails> findByUserId(Long userId) {
    return readers.stream().filter(r -> Objects.equals(r.getReader().getId(), userId)).findFirst();
  }

  @Override
  public int getCountFromCurrentYear() {
    return readers.size();
  }

  @Override
  public Iterable<ReaderDetails> findAll() {
    return List.copyOf(readers);
  }

  @Override
  public List<ReaderDetails> findByPhoneNumber(String phoneNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Page<ReaderDetails> findTopReaders(Pageable pageable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Page<ReaderBookCountDto> findTopByGenre(
      Pageable pageable, String genre, LocalDate startDate, LocalDate endDate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(ReaderDetails readerDetails) {
    readers.remove(readerDetails);
  }

  @Override
  public List<ReaderDetails> searchReaderDetails(
      pt.psoft.g1.psoftg1.shared.services.Page page, SearchReadersQuery query) {
    throw new UnsupportedOperationException();
  }
}
