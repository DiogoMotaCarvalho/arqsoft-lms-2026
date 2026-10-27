package pt.psoft.g1.psoftg1.lendingmanagement.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.lendingmanagement.model.policy.LendingPolicy;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;

/** The default provider stub returns the Standard policy for every reader, until #12. */
class DefaultLendingPolicyProviderTest {

  private DefaultLendingPolicyProvider provider;

  @BeforeEach
  void setUp() {
    LendingPolicyFactory factory = new LendingPolicyFactory();
    ReflectionTestUtils.setField(factory, "standardMaxActiveLendings", 3);
    ReflectionTestUtils.setField(factory, "standardLendingDurationInDays", 15);
    ReflectionTestUtils.setField(factory, "standardMaxRenewals", 3);
    ReflectionTestUtils.setField(factory, "standardRenewalDurationInDays", 15);
    provider = new DefaultLendingPolicyProvider(factory);
  }

  @Test
  void alwaysReturnsStandardPolicy() {
    LendingPolicy policy = provider.policyFor(mock(ReaderDetails.class));
    assertEquals("standard", policy.name());
  }

  @Test
  void toleratesNullReader() {
    assertEquals("standard", provider.policyFor(null).name());
  }
}
