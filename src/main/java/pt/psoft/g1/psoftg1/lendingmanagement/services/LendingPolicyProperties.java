package pt.psoft.g1.psoftg1.lendingmanagement.services;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The externalized lending-policy parameters (ARQSOFT P1, #9), bound from the standard Spring
 * configuration under the {@code lending.policy} prefix and validated at application startup against
 * each policy's constraints (lending spec §5.3). See ADR-0003.
 *
 * <p>Three sections mirror the domain — {@link Standard}, {@link Adaptive}, {@link Selection}.
 * Single-field bounds use JSR-380 annotations; the Adaptive/Selection cross-field ordering
 * constraints use {@code @AssertTrue} methods. This is the setup-time validation layer; the domain
 * policy constructors additionally fail fast (defense in depth), so the framework-free domain stays
 * self-protecting. Only {@code Standard} is consumed today; {@code Adaptive} is wired by #10 and
 * {@code Selection} by #12, but both are already bound and validated here.
 */
@ConfigurationProperties(prefix = "lending.policy")
@Validated
@Getter
public class LendingPolicyProperties {

  @Valid private final Standard standard = new Standard();
  @Valid private final Adaptive adaptive = new Adaptive();
  @Valid private final Selection selection = new Selection();

  /** Standard policy parameters (lending spec §2). */
  @Getter
  @Setter
  public static class Standard {
    @Positive private int maxActiveLendings;
    @Positive private int lendingDurationInDays;
    @PositiveOrZero private int maxRenewals;
    @Positive private int renewalDurationInDays;
  }

  /** Adaptive policy parameters (lending spec §3). */
  @Getter
  @Setter
  public static class Adaptive {
    @Positive private int maxActiveLendings;
    @Positive private int firstThreshold;
    @Positive private int secondThreshold;
    @Positive private int longPeriod;
    @Positive private int standardPeriod;
    @Positive private int shortPeriod;

    @AssertTrue(
        message =
            "adaptive thresholds must satisfy 0 < firstThreshold < secondThreshold < maxActiveLendings")
    public boolean isThresholdsStrictlyOrdered() {
      return 0 < firstThreshold
          && firstThreshold < secondThreshold
          && secondThreshold < maxActiveLendings;
    }

    @AssertTrue(
        message = "adaptive periods must satisfy longPeriod > standardPeriod > shortPeriod > 0")
    public boolean isPeriodsStrictlyOrdered() {
      return longPeriod > standardPeriod && standardPeriod > shortPeriod && shortPeriod > 0;
    }
  }

  /** Lending-policy selection criteria (lending spec §4). */
  @Getter
  @Setter
  public static class Selection {
    @Positive private int historyPeriodInMonths;
    @Positive private int minCompletedLendings;

    @Min(0)
    @Max(100)
    private int minOnTimeRatio;
  }
}
