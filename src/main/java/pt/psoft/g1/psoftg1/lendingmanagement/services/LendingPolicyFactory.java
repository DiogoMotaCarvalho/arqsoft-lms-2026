package pt.psoft.g1.psoftg1.lendingmanagement.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.StandardLendingPolicy;

/**
 * Builds {@link LendingPolicy} instances from externalized configuration, keeping config and
 * framework concerns out of the {@code model.policy} domain. Parameters are read from {@code
 * overrides.properties} (the externalized-config mechanism is formalized in #9); each policy
 * validates its own parameters at construction, so an invalid configuration fails fast on startup.
 */
@Component
@PropertySource({"classpath:config/overrides.properties"})
public class LendingPolicyFactory {

  @Value("${lending.policy.standard.maxActiveLendings:3}")
  private int standardMaxActiveLendings;

  @Value("${lending.policy.standard.lendingDurationInDays:15}")
  private int standardLendingDurationInDays;

  @Value("${lending.policy.standard.maxRenewals:3}")
  private int standardMaxRenewals;

  @Value("${lending.policy.standard.renewalDurationInDays:15}")
  private int standardRenewalDurationInDays;

  /**
   * Builds the policy with the given stable name.
   *
   * @throws IllegalArgumentException if no policy is registered under that name.
   */
  public LendingPolicy create(String name) {
    if (StandardLendingPolicy.NAME.equals(name)) {
      return standard();
    }
    throw new IllegalArgumentException("Unknown lending policy: " + name);
  }

  /** The configured Standard policy. */
  public LendingPolicy standard() {
    return new StandardLendingPolicy(
        standardMaxActiveLendings,
        standardLendingDurationInDays,
        standardMaxRenewals,
        standardRenewalDurationInDays);
  }
}
