package pt.psoft.g1.psoftg1.lendingmanagement.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.AdaptiveLendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.StandardLendingPolicy;

/**
 * Transparent-box tests for the config-reading factory: it builds valid Standard and Adaptive
 * policies from the {@link LendingPolicyProperties}, rejects unknown policy names, and fails fast
 * when the configuration violates a policy constraint (the domain constructor's defense-in-depth
 * layer).
 */
class LendingPolicyFactoryTest {

  /** A fully-populated, valid configuration for both policies. */
  private static LendingPolicyProperties validProps() {
    LendingPolicyProperties props = new LendingPolicyProperties();
    LendingPolicyProperties.Standard s = props.getStandard();
    s.setMaxActiveLendings(3);
    s.setLendingDurationInDays(15);
    s.setMaxRenewals(3);
    s.setRenewalDurationInDays(15);
    LendingPolicyProperties.Adaptive a = props.getAdaptive();
    a.setMaxActiveLendings(5);
    a.setFirstThreshold(2);
    a.setSecondThreshold(4);
    a.setLongPeriod(21);
    a.setStandardPeriod(14);
    a.setShortPeriod(7);
    return props;
  }

  private final LendingPolicyFactory factory = new LendingPolicyFactory(validProps());

  @Test
  void buildsStandardPolicyByName() {
    LendingPolicy policy = factory.create(StandardLendingPolicy.NAME);
    assertInstanceOf(StandardLendingPolicy.class, policy);
    assertEquals("standard", policy.name());
  }

  @Test
  void buildsAdaptivePolicyByName() {
    LendingPolicy policy = factory.create(AdaptiveLendingPolicy.NAME);
    assertInstanceOf(AdaptiveLendingPolicy.class, policy);
    assertEquals("adaptive", policy.name());
  }

  @Test
  void convenienceBuildersMatchCreate() {
    assertEquals(factory.standard().name(), factory.create(StandardLendingPolicy.NAME).name());
    assertEquals(factory.adaptive().name(), factory.create(AdaptiveLendingPolicy.NAME).name());
  }

  @Test
  void rejectsUnknownPolicyName() {
    assertThrows(IllegalArgumentException.class, () -> factory.create("nonexistent"));
  }

  @Test
  void failsFastOnInvalidStandardConfiguration() {
    LendingPolicyProperties props = validProps();
    props.getStandard().setMaxActiveLendings(0);
    LendingPolicyFactory invalid = new LendingPolicyFactory(props);
    assertThrows(IllegalArgumentException.class, invalid::standard);
  }

  @Test
  void failsFastOnInvalidAdaptiveConfiguration() {
    LendingPolicyProperties props = validProps();
    props.getAdaptive().setSecondThreshold(5); // not < maxActiveLendings (5)
    LendingPolicyFactory invalid = new LendingPolicyFactory(props);
    assertThrows(IllegalArgumentException.class, invalid::adaptive);
  }
}
