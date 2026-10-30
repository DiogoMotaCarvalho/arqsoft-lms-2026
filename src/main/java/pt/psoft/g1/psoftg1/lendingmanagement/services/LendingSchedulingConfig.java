package pt.psoft.g1.psoftg1.lendingmanagement.services;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring scheduling so the {@link OverdueNotificationScheduler} overdue sweep runs on its
 * configured cadence (issue #18, ADR-0014).
 */
@Configuration
@EnableScheduling
public class LendingSchedulingConfig {}
