package pt.psoft.g1.psoftg1.shared.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ConcurrencyServiceTest {
  private final ConcurrencyService service = new ConcurrencyService();

  @Test
  void ensureUnquotedVersionIsParsed() {
    assertEquals(3L, service.getVersionFromIfMatchHeader("3"));
  }

  @Test
  void ensureQuotedVersionIsParsed() {
    assertEquals(12L, service.getVersionFromIfMatchHeader("\"12\""));
  }

  @Test
  void ensureNonNumericVersionIsRejected() {
    assertThrows(NumberFormatException.class, () -> service.getVersionFromIfMatchHeader("abc"));
  }
}
