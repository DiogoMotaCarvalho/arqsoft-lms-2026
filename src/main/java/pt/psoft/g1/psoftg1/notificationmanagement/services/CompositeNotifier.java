package pt.psoft.g1.psoftg1.notificationmanagement.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import pt.psoft.g1.psoftg1.notificationmanagement.infrastructure.NotificationRecord;
import pt.psoft.g1.psoftg1.notificationmanagement.infrastructure.NotificationRecordRepository;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * The configured composite notifier (issue #22, ADR-0016): the single collaborator the after-commit
 * listener calls. Spring injects every {@link NotificationChannel} bean plus the selection
 * configuration; it indexes the channels by {@link NotificationChannel#type()} and keeps the
 * configured active ones in order (defaulting to all registered channels when {@code
 * notifications.channels} is empty). A configured name with no matching channel fails fast at
 * construction, so a typo fails startup rather than silently never notifying a reader.
 *
 * <p>On each event it fans the notification out to every active channel, deliberately unlike the
 * bibliographic first-present-wins selection, because a reader should be reached by each configured
 * means. Each attempt is recorded as one {@link NotificationRecord}: a {@code SENT} or {@code FAILED}
 * outcome is persisted, a quiet skip is not. A channel's failure is isolated so the others still run,
 * and the whole feature can be turned off with {@code notifications.enabled}.
 */
@Service
public class CompositeNotifier implements Notifier {

  private final List<NotificationChannel> activeInOrder;
  private final boolean enabled;
  private final NotificationRecordRepository auditRepository;

  public CompositeNotifier(
      List<NotificationChannel> channels,
      NotificationSelectionProperties properties,
      NotificationRecordRepository auditRepository) {
    this.enabled = properties.isEnabled();
    this.auditRepository = auditRepository;

    final Map<String, NotificationChannel> byType = new LinkedHashMap<>();
    for (NotificationChannel channel : channels) {
      byType.put(channel.type(), channel);
    }

    final List<String> configured = properties.getChannels();
    if (configured == null || configured.isEmpty()) {
      this.activeInOrder = new ArrayList<>(byType.values()); // default: all, in discovery order
      return;
    }

    final List<NotificationChannel> active = new ArrayList<>();
    for (String name : configured) {
      final NotificationChannel channel = byType.get(name);
      if (channel == null) {
        throw new IllegalArgumentException(
            "Unknown notification channel configured: '"
                + name
                + "'. Available channels: "
                + byType.keySet());
      }
      active.add(channel);
    }
    this.activeInOrder = active;
  }

  @Override
  public void notify(Notification notification) {
    if (!enabled) {
      return;
    }
    for (NotificationChannel channel : activeInOrder) {
      DeliveryResult result;
      try {
        result = channel.send(notification);
      } catch (RuntimeException e) {
        // Defensive: a channel reports a delivery failure as a result, so an exception here is an
        // unexpected fault. Isolate it so the remaining channels still run, and record it as failed.
        result = DeliveryResult.failed(null, e.getMessage());
      }
      record(notification, channel.type(), result);
    }
  }

  private void record(Notification notification, String mechanism, DeliveryResult result) {
    if (result.status() == DeliveryStatus.SKIPPED) {
      return; // a skip is not a delivery attempt
    }
    final NotificationRecord.Outcome outcome =
        result.status() == DeliveryStatus.SENT
            ? NotificationRecord.Outcome.SENT
            : NotificationRecord.Outcome.FAILED;
    auditRepository.save(
        new NotificationRecord(
            notification.getEventType(),
            mechanism,
            notification.getReaderNumber(),
            result.recipient(),
            notification.getLendingNumber(),
            outcome,
            result.failureReason()));
  }
}
