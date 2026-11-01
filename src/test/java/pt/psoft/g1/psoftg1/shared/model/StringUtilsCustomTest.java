package pt.psoft.g1.psoftg1.shared.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StringUtilsCustomTest {
  @Test
  void ensureLettersDigitsSpacesAccentsApostrophesAndHyphensAreAlphanumeric() {
    assertTrue(StringUtilsCustom.isAlphanumeric("João d'Ávila-Silva 2"));
    assertTrue(StringUtilsCustom.isAlphanumeric(""));
  }

  @Test
  void ensureSymbolsAreNotAlphanumeric() {
    assertFalse(StringUtilsCustom.isAlphanumeric("joao@mail"));
    assertFalse(StringUtilsCustom.isAlphanumeric("<script>"));
  }

  @Test
  void ensureSanitizeHtmlRemovesScripts() {
    String sanitized = StringUtilsCustom.sanitizeHtml("<b>bold</b><script>alert(1)</script>");

    assertEquals("<b>bold</b>", sanitized);
  }

  @Test
  void ensureSanitizeHtmlRemovesEventHandlers() {
    String sanitized = StringUtilsCustom.sanitizeHtml("<b onclick=\"steal()\">bold</b>");

    assertEquals("<b>bold</b>", sanitized);
  }

  @Test
  void ensureSanitizeHtmlKeepsPlainText() {
    assertEquals("plain text", StringUtilsCustom.sanitizeHtml("plain text"));
  }
}
