package pt.psoft.g1.psoftg1.lendingmanagement.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.StandardLendingPolicy;

/**
 * Transparent-box tests for the config-reading factory: it builds a valid Standard policy from its
 * parameters, rejects unknown policy names, and fails fast when configured with invalid parameters.
 */
class LendingPolicyFactoryTest {

  private LendingPolicyFactory factory;

  @BeforeEach
  void setUp() {
    factory = new LendingPolicyFactory();
    setParams(3, 15, 3, 15);
  }

  private void setParams(int maxActive, int duration, int maxRenewals, int renewalDuration) {
    ReflectionTestUtils.setField(factory, "standardMaxActiveLendings", maxActive);
    ReflectionTestUtils.setField(factory, "standardLendingDurationInDays", duration);
    ReflectionTestUtils.setField(factory, "standardMaxRenewals", maxRenewals);
    ReflectionTestUtils.setField(factory, "standardRenewalDurationInDays", renewalDuration);
  }

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
    setParams(0, 15, 3, 15);
    assertThrows(IllegalArgumentException.class, () -> factory.standard());
  }
}
