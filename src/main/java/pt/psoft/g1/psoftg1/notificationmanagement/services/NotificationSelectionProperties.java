package pt.psoft.g1.psoftg1.notificationmanagement.services;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized notification mechanism selection (issue #22, ADR-0016), bound from the {@code
 * notifications} prefix. {@code channels} is an ordered list of mechanism names (for example {@code
 * email,sms,webhook}): one name is a single-mechanism setup, several names enable several at once,
 * and an empty list defaults to all registered channels, mirroring {@code bibliographic.sources}
 * (ADR-0012). Unlike bibliographic selection, the composite notifier fans out to all active
 * channels rather than stopping at the first. A name with no matching channel fails startup. {@code
 * enabled} is a global switch that turns all notifications off, mirroring {@code
 * bibliographic.enrichment.enabled}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "notifications")
public class NotificationSelectionProperties {

  /** Global switch for the whole notification feature. */
  private boolean enabled = true;

  /** Ordered names of the active mechanisms. Empty means all registered channels. */
  private List<String> channels = new ArrayList<>();
}
