package pt.psoft.g1.psoftg1.lendingmanagement.services;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.exceptions.LendingForbiddenException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEvent;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.ReaderLendingContext;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.RenewalContext;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.services.Page;

@Service
@RequiredArgsConstructor
@PropertySource({"classpath:config/library.properties"})
public class LendingServiceImpl implements LendingService {
  private final LendingRepository lendingRepository;
  private final FineRepository fineRepository;
  private final BookRepository bookRepository;
  private final ReaderRepository readerRepository;
  private final LendingPolicyProvider lendingPolicyProvider;
  private final ApplicationEventPublisher eventPublisher;

  @Value("${fineValuePerDayInCents}")
  private int fineValuePerDayInCents;

  @Override
  public Optional<Lending> findByLendingNumber(String lendingNumber) {
    return lendingRepository.findByLendingNumber(lendingNumber);
  }

  @Override
  public List<Lending> listByReaderNumberAndIsbn(
      String readerNumber, String isbn, Optional<Boolean> returned) {
    List<Lending> lendings = lendingRepository.listByReaderNumberAndIsbn(readerNumber, isbn);
    if (returned.isEmpty()) {
      return lendings;
    } else {
      for (int i = 0; i < lendings.size(); i++) {
        if ((lendings.get(i).getReturnedDate() == null) == returned.get()) {
          lendings.remove(i);
          i--;
        }
      }
    }
    return lendings;
  }

  @Override
  @Transactional
  public Lending create(final CreateLendingRequest resource) {
    final var b =
        bookRepository
            .findByIsbn(resource.getIsbn())
            .orElseThrow(() -> new NotFoundException("Book not found"));
    final var r =
        readerRepository
            .findByReaderNumber(resource.getReaderNumber())
            .orElseThrow(() -> new NotFoundException("Reader not found"));

    // Delegate the borrow-eligibility and lending-period decisions to the applicable policy;
    // the service only gathers the reader's situation and maps a denial to the exception.
    final ReaderLendingContext context = buildReaderLendingContext(resource.getReaderNumber());
    final LendingPolicy policy = lendingPolicyProvider.policyFor(r);

    policy
        .canBorrow(context)
        .getReason()
        .ifPresent(
            reason -> {
              throw new LendingForbiddenException(reason);
            });

    int seq = lendingRepository.getCountFromCurrentYear() + 1;
    final LocalDate limitDate = policy.limitDateFor(LocalDate.now(), context);
    final Lending l = new Lending(b, r, seq, limitDate, fineValuePerDayInCents);

    final Lending saved = lendingRepository.save(l);
    eventPublisher.publishEvent(LendingEvent.of(LendingEventType.CREATED, saved));
    return saved;
  }

  /**
   * Summarizes the reader's active lendings (not yet returned, overdue included) into the data a
   * {@link LendingPolicy} needs to decide a new lending.
   */
  private ReaderLendingContext buildReaderLendingContext(String readerNumber) {
    int activeLendings = 0;
    boolean hasOverdue = false;
    for (Lending lending : lendingRepository.listOutstandingByReaderNumber(readerNumber)) {
      activeLendings++;
      if (lending.getDaysDelayed() > 0) {
        hasOverdue = true;
      }
    }
    return new ReaderLendingContext(activeLendings, hasOverdue);
  }

  @Override
  @Transactional
  public Lending setReturned(
      final String lendingNumber,
      final SetLendingReturnedRequest resource,
      final long desiredVersion) {

    var lending =
        lendingRepository
            .findByLendingNumber(lendingNumber)
            .orElseThrow(
                () -> new NotFoundException("Cannot update lending with this lending number"));

    lending.setReturned(desiredVersion, resource.getCommentary());

    if (lending.getDaysDelayed() > 0) {
      final var fine = new Fine(lending);
      fineRepository.save(fine);
    }

    final Lending saved = lendingRepository.save(lending);
    eventPublisher.publishEvent(LendingEvent.of(LendingEventType.RETURNED, saved));
    return saved;
  }

  @Override
  @Transactional
  public Lending renew(final String lendingNumber, final long desiredVersion) {

    final var lending =
        lendingRepository
            .findByLendingNumber(lendingNumber)
            .orElseThrow(
                () -> new NotFoundException("Cannot renew lending with this lending number"));

    // Re-evaluate the applicable policy at renewal time (lending spec §4.3), then delegate the
    // renewal decision and the renewed limit date to it; the service only gathers the situation
    // and maps a denial to the exception. The state change itself stays on the entity (ADR-0007).
    final LendingPolicy policy = lendingPolicyProvider.policyFor(lending.getReaderDetails());
    final RenewalContext context = buildRenewalContext(lending);

    policy
        .canRenew(context)
        .getReason()
        .ifPresent(
            reason -> {
              throw new LendingForbiddenException(reason);
            });

    lending.renew(desiredVersion, policy.renewalLimitDateFor(context));

    final Lending saved = lendingRepository.save(lending);
    eventPublisher.publishEvent(LendingEvent.of(LendingEventType.RENEWED, saved));
    return saved;
  }

  /**
   * Summarizes the facts a {@link LendingPolicy} needs to decide a renewal: the lending's current
   * limit date, how many times it has already been renewed, whether it is currently overdue, and
   * how many active lendings the reader holds (the lending being renewed is itself active and is
   * therefore included in that count).
   */
  private RenewalContext buildRenewalContext(final Lending lending) {
    final boolean overdue = lending.getDaysDelayed() > 0;
    final int activeLendings =
        lendingRepository.listOutstandingByReaderNumber(lending.getReaderNumber()).size();
    return new RenewalContext(
        lending.getLimitDate(), lending.getRenewalCount(), overdue, activeLendings);
  }

  @Override
  public Double getAverageDuration() {
    Double avg = lendingRepository.getAverageDuration();
    return Double.valueOf(String.format(Locale.US, "%.1f", avg));
  }

  @Override
  public List<Lending> getOverdue(Page page) {
    if (page == null) {
      page = new Page(1, 10);
    }
    return lendingRepository.getOverdue(page);
  }

  @Override
  public Double getAvgLendingDurationByIsbn(String isbn) {
    Double avg = lendingRepository.getAvgLendingDurationByIsbn(isbn);
    return Double.valueOf(String.format(Locale.US, "%.1f", avg));
  }

  @Override
  public List<Lending> searchLendings(Page page, SearchLendingQuery query) {
    LocalDate startDate = null;
    LocalDate endDate = null;

    if (page == null) {
      page = new Page(1, 10);
    }
    if (query == null) {
      query = new SearchLendingQuery("", "", null, LocalDate.now().minusDays(10L).toString(), null);
    }

    try {
      if (query.getStartDate() != null) {
        startDate = LocalDate.parse(query.getStartDate());
      }
      if (query.getEndDate() != null) {
        endDate = LocalDate.parse(query.getEndDate());
      }
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Expected format is YYYY-MM-DD");
    }

    return lendingRepository.searchLendings(
        page, query.getReaderNumber(), query.getIsbn(), query.getReturned(), startDate, endDate);
  }
}
