package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The default, self-contained {@link EmailGateway} (issue #19, ADR-0015): it records the delivery
 * to the log rather than talking to an SMTP server, so the email mechanism runs and is demonstrable
 * end to end with no external account or credentials. A real SMTP sender (for example over Spring
 * {@code JavaMailSender}) is a later alternative implementation of this same interface, selected by
 * configuration, with no change to {@link EmailNotificationChannel}.
 */
@Component
public class LoggingEmailGateway implements EmailGateway {

  private static final Logger log = LoggerFactory.getLogger(LoggingEmailGateway.class);

  @Override
  public void send(EmailMessage message) {
    log.info("EMAIL to {} | {} | {}", message.getTo(), message.getSubject(), message.getBody());
  }
}
