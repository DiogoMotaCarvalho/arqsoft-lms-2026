package pt.psoft.g1.psoftg1.notificationmanagement;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEvent;
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;
import pt.psoft.g1.psoftg1.notificationmanagement.services.DeliveryResult;
import pt.psoft.g1.psoftg1.notificationmanagement.services.NotificationChannel;

/**
 * End-to-end test of the notification trigger through the real Spring wiring (issue #18, ADR-0014),
 * the evidence that the after-commit, asynchronous delivery works (C2.1) and that a new mechanism
 * is an ordinary channel bean the listener fans out to (C2.2). It is deliberately not transactional
 * at the class level, so a real transaction commits (or rolls back) and the
 * {@code @TransactionalEventListener} fires accordingly. A test channel registered as an ordinary
 * {@link NotificationChannel} bean records what is delivered. Events are published directly through
 * a committed and a rolled-back transaction, which is how {@code LendingServiceImpl} and the
 * overdue sweep publish them, so the test exercises the trigger contract without depending on
 * committed entity fixtures. Two outcomes are asserted: an event published in a committed
 * transaction is rendered and delivered after commit and off the publishing thread, and an event
 * published in a transaction that rolls back is never delivered.
 */
@SpringBootTest
@Import(NotificationTriggerIntegrationTest.RecordingChannelConfig.class)
class NotificationTriggerIntegrationTest {

  /** A notification together with the thread it was delivered on, for the off-thread assertion. */
  record Delivery(Notification notification, Thread thread) {}

  /** A test channel that records every notification handed to it, added alongside the real ones. */
  static final class RecordingChannel implements NotificationChannel {
    final List<Delivery> delivered = new CopyOnWriteArrayList<>();

    @Override
    public DeliveryResult send(Notification notification) {
      delivered.add(new Delivery(notification, Thread.currentThread()));
      return DeliveryResult.sent(notification.getReaderNumber());
    }

    @Override
    public String type() {
      return "recording-test";
    }
  }

  @TestConfiguration
  static class RecordingChannelConfig {
    @Bean
    RecordingChannel recordingChannel() {
      return new RecordingChannel();
    }
  }

  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private RecordingChannel recordingChannel;

  private TransactionTemplate tx;

  @BeforeEach
  void setUp() {
    recordingChannel.delivered.clear();
    tx = new TransactionTemplate(transactionManager);
  }

  private static LendingEvent event(LendingEventType type, String lendingNumber) {
    return new LendingEvent(
        type,
        "2024/770",
        "reader@gmail.com",
        "919191919",
        lendingNumber,
        LocalDate.of(2026, 1, 10),
        LocalDate.of(2026, 1, 25),
        null);
  }

  @Test
  void anEventPublishedInACommittedTransactionIsRenderedAndDeliveredAfterCommit() {
    final Thread publishingThread = Thread.currentThread();

    tx.executeWithoutResult(
        status -> eventPublisher.publishEvent(event(LendingEventType.CREATED, "2026/5")));

    await()
        .atMost(5, SECONDS)
        .untilAsserted(
            () ->
                assertThat(recordingChannel.delivered)
                    .anySatisfy(
                        d -> {
                          final Notification n = d.notification();
                          assertThat(n.getEventType()).isEqualTo(LendingEventType.CREATED);
                          assertThat(n.getLendingNumber()).isEqualTo("2026/5");
                          assertThat(n.getEmail()).isEqualTo("reader@gmail.com");
                          assertThat(n.getSubject()).isEqualTo("New lending 2026/5");
                        }));

    // Delivery ran off the publishing thread (asynchronous delivery): the channel captured the
    // thread it was actually invoked on, and it is not the thread that published the event.
    assertThat(recordingChannel.delivered)
        .allSatisfy(d -> assertThat(d.thread()).isNotSameAs(publishingThread));
  }

  @Test
  void anEventPublishedInARolledBackTransactionIsNeverDelivered() {
    final String lendingNumber = "9999/777";

    try {
      tx.executeWithoutResult(
          status -> {
            eventPublisher.publishEvent(event(LendingEventType.OVERDUE, lendingNumber));
            throw new IllegalStateException("force rollback");
          });
    } catch (IllegalStateException expected) {
      // the transaction rolled back, so the after-commit listener must not run
    }

    await()
        .during(1, SECONDS)
        .atMost(2, SECONDS)
        .untilAsserted(
            () ->
                assertThat(recordingChannel.delivered)
                    .noneMatch(d -> lendingNumber.equals(d.notification().getLendingNumber())));
  }
}
