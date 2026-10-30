package pt.psoft.g1.psoftg1.notificationmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import pt.psoft.g1.psoftg1.notificationmanagement.infrastructure.NotificationRecordRepository;
import pt.psoft.g1.psoftg1.notificationmanagement.model.Notification;

/**
 * Context-binding test for the mechanism selection configuration (issue #22, ADR-0016), in the style
 * of the bibliographic source-selection test. It asserts the setup-time outcome: a valid {@code
 * notifications.channels} list binds and the context starts, an empty list binds and defaults to all
 * channels, and a name with no matching channel fails context startup (the fail-fast validation in
 * the composite's construction), so a typo becomes a boot failure rather than a silently un-notified
 * reader.
 */
class CompositeNotifierSelectionConfigTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(StubChannels.class);

  @Test
  void aValidChannelListBindsAndTheContextStarts() {
    runner
        .withPropertyValues("notifications.channels=email,sms")
        .run(ctx -> assertThat(ctx).hasNotFailed());
  }

  @Test
  void anEmptyChannelListBindsAndDefaultsToAll() {
    runner.run(ctx -> assertThat(ctx).hasNotFailed());
  }

  @Test
  void anUnknownChannelNameFailsContextStartup() {
    runner
        .withPropertyValues("notifications.channels=does-not-exist")
        .run(ctx -> assertThat(ctx).hasFailed());
  }

  /** A fixed-type channel double that delivers nothing. */
  private static NotificationChannel channel(String type) {
    return new NotificationChannel() {
      @Override
      public DeliveryResult send(Notification notification) {
        return DeliveryResult.sent(type);
      }

      @Override
      public String type() {
        return type;
      }
    };
  }

  /** Two stub channel beans plus the composite under the selection properties. */
  @EnableConfigurationProperties(NotificationSelectionProperties.class)
  static class StubChannels {

    @Bean
    NotificationChannel email() {
      return channel("email");
    }

    @Bean
    NotificationChannel sms() {
      return channel("sms");
    }

    @Bean
    NotificationRecordRepository auditRepository() {
      return mock(NotificationRecordRepository.class);
    }

    @Bean
    CompositeNotifier compositeNotifier(
        List<NotificationChannel> channels,
        NotificationSelectionProperties properties,
        NotificationRecordRepository auditRepository) {
      return new CompositeNotifier(channels, properties, auditRepository);
    }
  }
}
