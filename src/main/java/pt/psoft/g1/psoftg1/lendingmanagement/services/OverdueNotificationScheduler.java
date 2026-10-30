package pt.psoft.g1.psoftg1.lendingmanagement.services;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEvent;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;

/**
 * Detects lendings that have become overdue and notifies the reader (issue #18, ADR-0014). A
 * becoming-overdue has no user action to hang a trigger on, so it is detected by a scheduled sweep
 * rather than on read. The cadence is bound from configuration ({@code
 * notifications.overdue.check-cron}), so it is changed without a code edit. Each run selects the
 * lendings that are now overdue and not yet notified, publishes one overdue {@link LendingEvent}
 * for each through the same {@code ApplicationEventPublisher} the lending flow uses, and sets the
 * once-only marker on the lending so a later run does not notify the same lending again.
 */
@Component
@RequiredArgsConstructor
public class OverdueNotificationScheduler {

  private static final Logger log = LoggerFactory.getLogger(OverdueNotificationScheduler.class);

  private final LendingRepository lendingRepository;
  private final ApplicationEventPublisher eventPublisher;

  /**
   * Runs the sweep in a transaction so that the overdue events, published through the same
   * after-commit listener as the lending flow, are delivered only once the once-only markers have
   * committed. Without an active transaction the {@code @TransactionalEventListener(AFTER_COMMIT)}
   * would discard the published events (its default {@code fallbackExecution=false}).
   */
  @Scheduled(cron = "${notifications.overdue.check-cron}")
  @Transactional
  public void checkOverdue() {
    final int notified = sweep(LocalDate.now());
    if (notified > 0) {
      log.info("Overdue sweep notified {} lending(s)", notified);
    }
  }

  /**
   * Publishes an overdue event for each overdue-and-not-yet-notified lending as of {@code today}
   * and marks each so it is not notified again, returning how many were notified. Package-private
   * so the sweep can be unit-tested without a scheduler.
   */
  int sweep(LocalDate today) {
    final List<Lending> overdue = lendingRepository.listOverdueNotNotified(today);
    for (Lending lending : overdue) {
      eventPublisher.publishEvent(LendingEvent.of(LendingEventType.OVERDUE, lending));
      lending.markOverdueNotified();
      lendingRepository.save(lending);
    }
    return overdue.size();
  }
}
