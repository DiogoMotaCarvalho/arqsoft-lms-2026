package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The default, self-contained {@link SmsGateway} (issue #20, ADR-0015): it records the delivery to
 * the log rather than calling an SMS vendor, so the SMS mechanism runs and is demonstrable end to end
 * with no vendor account. A real vendor API is a later alternative implementation of this same
 * interface, selected by configuration, with no change to {@link SmsNotificationChannel}.
 */
@Component
public class LoggingSmsGateway implements SmsGateway {

  private static final Logger log = LoggerFactory.getLogger(LoggingSmsGateway.class);

  @Override
  public void send(SmsMessage message) {
    log.info("SMS to {} | {}", message.getTo(), message.getBody());
  }
}
