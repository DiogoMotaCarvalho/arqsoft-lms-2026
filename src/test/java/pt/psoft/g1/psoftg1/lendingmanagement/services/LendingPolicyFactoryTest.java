package pt.psoft.g1.psoftg1.lendingmanagement.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.StandardLendingPolicy;

/**
 * Transparent-box tests for the config-reading factory: it builds a valid Standard policy from the
 * {@link LendingPolicyProperties}, rejects unknown policy names, and fails fast when the
 * configuration violates a policy constraint (the domain constructor's defense-in-depth layer).
 */
class LendingPolicyFactoryTest {

  private static LendingPolicyProperties standardProps(
      int maxActive, int duration, int maxRenewals, int renewalDuration) {
    LendingPolicyProperties props = new LendingPolicyProperties();
    LendingPolicyProperties.Standard s = props.getStandard();
    s.setMaxActiveLendings(maxActive);
    s.setLendingDurationInDays(duration);
    s.setMaxRenewals(maxRenewals);
    s.setRenewalDurationInDays(renewalDuration);
    return props;
  }

  private final LendingPolicyFactory factory =
      new LendingPolicyFactory(standardProps(3, 15, 3, 15));

  @Test
  void buildsStandardPolicyByName() {
    LendingPolicy policy = factory.create(StandardLendingPolicy.NAME);
    assertInstanceOf(StandardLendingPolicy.class, policy);
    assertEquals("standard", policy.name());
  }

  @Test
  void standardConvenienceMatchesCreate() {
    assertEquals(factory.standard().name(), factory.create(StandardLendingPolicy.NAME).name());
  }

  @Test
  void rejectsUnknownPolicyName() {
    assertThrows(IllegalArgumentException.class, () -> factory.create("adaptive"));
  }

  @Test
  void failsFastOnInvalidConfiguration() {
    LendingPolicyFactory invalid = new LendingPolicyFactory(standardProps(0, 15, 3, 15));
    assertThrows(IllegalArgumentException.class, invalid::standard);
  }
}
