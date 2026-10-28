package pt.psoft.g1.psoftg1.lendingmanagement.services;

import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.StandardLendingPolicy;

/**
 * Builds {@link LendingPolicy} instances from the externalized, validated configuration
 * ({@link LendingPolicyProperties}), keeping config and framework concerns out of the {@code
 * model.policy} domain. The configuration is bound and validated at startup (#9, ADR-0003); each
 * policy additionally validates its own parameters at construction, so an invalid configuration
 * fails fast.
 *
 * <p>Only the Standard policy is built today; the {@code "adaptive"} branch is added by #10.
 */
@Component
public class LendingPolicyFactory {

  private final LendingPolicyProperties properties;

  public LendingPolicyFactory(LendingPolicyProperties properties) {
    this.properties = properties;
  }

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
    final LendingPolicyProperties.Standard s = properties.getStandard();
    return new StandardLendingPolicy(
        s.getMaxActiveLendings(),
        s.getLendingDurationInDays(),
        s.getMaxRenewals(),
        s.getRenewalDurationInDays());
  }
}
