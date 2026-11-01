package pt.psoft.g1.psoftg1.lendingmanagement.model.policy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PolicyDecisionTest {

  @Test
  void allowedHasNoReason() {
    PolicyDecision decision = PolicyDecision.allowed();
    assertTrue(decision.isAllowed());
    assertFalse(decision.isDenied());
    assertEquals(Optional.empty(), decision.getReason());
  }

  @Test
  void deniedCarriesReason() {
    PolicyDecision decision = PolicyDecision.denied("too many books");
    assertTrue(decision.isDenied());
    assertFalse(decision.isAllowed());
    assertEquals(Optional.of("too many books"), decision.getReason());
  }

  @Test
  void denialRequiresNonBlankReason() {
    assertThrows(IllegalArgumentException.class, () -> PolicyDecision.denied(null));
    assertThrows(IllegalArgumentException.class, () -> PolicyDecision.denied(""));
    assertThrows(IllegalArgumentException.class, () -> PolicyDecision.denied("  "));
  }

  @Test
  void valueEquality() {
    assertEquals(PolicyDecision.allowed(), PolicyDecision.allowed());
    assertEquals(PolicyDecision.denied("x"), PolicyDecision.denied("x"));
    assertNotEquals(PolicyDecision.denied("x"), PolicyDecision.denied("y"));
    assertNotEquals(PolicyDecision.allowed(), PolicyDecision.denied("x"));
  }
}
