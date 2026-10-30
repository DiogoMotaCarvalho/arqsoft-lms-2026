package pt.psoft.g1.psoftg1.notificationmanagement.infrastructure;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link WebhookProperties} so the webhook adapter's config binds at startup (issue #21). */
@Configuration
@EnableConfigurationProperties(WebhookProperties.class)
public class WebhookConfig {}
