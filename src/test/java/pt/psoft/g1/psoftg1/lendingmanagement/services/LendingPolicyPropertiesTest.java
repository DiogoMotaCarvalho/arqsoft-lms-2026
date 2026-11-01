package pt.psoft.g1.psoftg1.lendingmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.ConfigurationPropertiesBindException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Opaque-box tests of the externalized-config mechanism (#9, ADR-0003) at its highest seam: binding
 * {@link LendingPolicyProperties} from external properties and validating it at context startup.
 * Asserts the <em>outcome</em> of the configuration contract — a valid configuration binds and the
 * context starts; each single-field and cross-field constraint violation prevents startup — rather
 * than any internal detail. This is the setup-time "config validated against each policy's
 * constraints" evidence (lending spec §5.3).
 */
class LendingPolicyPropertiesTest {

  /** A fully-specified configuration that satisfies every constraint. */
  private static final String[] VALID = {
    "lending.policy.standard.maxActiveLendings=3",
    "lending.policy.standard.lendingDurationInDays=15",
    "lending.policy.standard.maxRenewals=3",
    "lending.policy.standard.renewalDurationInDays=15",
    "lending.policy.adaptive.maxActiveLendings=5",
    "lending.policy.adaptive.firstThreshold=2",
    "lending.policy.adaptive.secondThreshold=4",
    "lending.policy.adaptive.longPeriod=21",
    "lending.policy.adaptive.standardPeriod=14",
    "lending.policy.adaptive.shortPeriod=7",
    "lending.policy.selection.historyPeriodInMonths=12",
    "lending.policy.selection.minCompletedLendings=5",
    "lending.policy.selection.minOnTimeRatio=80",
  };

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner().withUserConfiguration(EnableProps.class);

  /** The valid baseline plus overrides for the keys a case wants to change (later value wins). */
  private static String[] with(String... overrides) {
    String[] merged = Arrays.copyOf(VALID, VALID.length + overrides.length);
    System.arraycopy(overrides, 0, merged, VALID.length, overrides.length);
    return merged;
  }

  // --- valid baseline ---

  @Test
  void validConfigurationBindsAndStarts() {
    runner
        .withPropertyValues(VALID)
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              LendingPolicyProperties props = context.getBean(LendingPolicyProperties.class);
              assertThat(props.getStandard().getMaxActiveLendings()).isEqualTo(3);
              assertThat(props.getAdaptive().getSecondThreshold()).isEqualTo(4);
              assertThat(props.getSelection().getMinOnTimeRatio()).isEqualTo(80);
            });
  }

  // --- single-field boundaries ---

  @Test
  void rejectsNonPositiveStandardMaxActiveLendings() {
    assertStartupFails(with("lending.policy.standard.maxActiveLendings=0"));
  }

  @Test
  void rejectsNonPositiveLendingPeriod() {
    assertStartupFails(with("lending.policy.standard.lendingDurationInDays=0"));
  }

  @Test
  void rejectsNegativeMaxRenewals() {
    assertStartupFails(with("lending.policy.standard.maxRenewals=-1"));
  }

  @Test
  void rejectsOnTimeRatioBelowZero() {
    assertStartupFails(with("lending.policy.selection.minOnTimeRatio=-1"));
  }

  @Test
  void rejectsOnTimeRatioAboveHundred() {
    assertStartupFails(with("lending.policy.selection.minOnTimeRatio=101"));
  }

  // --- adaptive cross-field ordering ---

  @Test
  void rejectsFirstThresholdEqualToSecondThreshold() {
    assertStartupFails(
        with(
            "lending.policy.adaptive.firstThreshold=4",
            "lending.policy.adaptive.secondThreshold=4"));
  }

  @Test
  void rejectsSecondThresholdEqualToMaxActiveLendings() {
    assertStartupFails(with("lending.policy.adaptive.secondThreshold=5")); // maxActiveLendings=5
  }

  @Test
  void rejectsLongPeriodEqualToStandardPeriod() {
    assertStartupFails(
        with("lending.policy.adaptive.longPeriod=14", "lending.policy.adaptive.standardPeriod=14"));
  }

  @Test
  void rejectsInvertedAdaptivePeriods() {
    assertStartupFails(
        with(
            "lending.policy.adaptive.longPeriod=7",
            "lending.policy.adaptive.standardPeriod=14",
            "lending.policy.adaptive.shortPeriod=21"));
  }

  private void assertStartupFails(String[] properties) {
    runner
        .withPropertyValues(properties)
        .run(
            context ->
                assertThat(context)
                    .hasFailed()
                    .getFailure()
                    .isInstanceOf(ConfigurationPropertiesBindException.class));
  }

  /** Minimal configuration that registers the properties bean for binding + validation. */
  @EnableConfigurationProperties(LendingPolicyProperties.class)
  static class EnableProps {}
}
