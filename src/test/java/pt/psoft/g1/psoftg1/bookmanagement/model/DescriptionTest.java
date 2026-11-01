package pt.psoft.g1.psoftg1.bookmanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DescriptionTest {
  private static final int MAX_LENGTH = 4096;

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\n\t"})
  void ensureMissingDescriptionIsStoredAsNull(String value) {
    assertNull(new Description(value).toString());
  }

  @Test
  void ensureDescriptionAcceptsMaximumLength() {
    assertEquals(MAX_LENGTH, new Description("a".repeat(MAX_LENGTH)).toString().length());
  }

  @Test
  void ensureDescriptionMustNotBeOversize() {
    assertThrows(IllegalArgumentException.class, () -> new Description("a".repeat(MAX_LENGTH + 1)));
  }

  @Test
  void ensureDescriptionIsSanitized() {
    final var description = new Description("<b>Great</b> book<script>alert(1)</script>");
    assertEquals("<b>Great</b> book", description.toString());
  }

  @Test
  void ensureDescriptionIsSet() {
    final var description = new Description("Some description");
    assertEquals("Some description", description.toString());
  }

  @Test
  void ensureDescriptionIsChanged() {
    final var description = new Description("Some description");
    description.setDescription("Some other description");
    assertEquals("Some other description", description.toString());
  }

  @Test
  void ensureDescriptionCanBeCleared() {
    final var description = new Description("Some description");
    description.setDescription(null);
    assertNull(description.toString());
  }
}
