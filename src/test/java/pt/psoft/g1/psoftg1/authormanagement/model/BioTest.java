package pt.psoft.g1.psoftg1.authormanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BioTest {
  private static final int MAX_LENGTH = 4096;

  @Test
  void ensureBioMustNotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new Bio(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\n\t"})
  void ensureBioMustNotBeBlank(String bio) {
    assertThrows(IllegalArgumentException.class, () -> new Bio(bio));
  }

  @Test
  void ensureBioAcceptsMaximumLength() {
    assertEquals(MAX_LENGTH, new Bio("a".repeat(MAX_LENGTH)).toString().length());
  }

  @Test
  void ensureBioMustNotBeOversize() {
    assertThrows(IllegalArgumentException.class, () -> new Bio("a".repeat(MAX_LENGTH + 1)));
  }

  @Test
  void ensureBioIsSanitized() {
    final var bio = new Bio("<i>Poet</i><script>alert(1)</script>");
    assertEquals("<i>Poet</i>", bio.toString());
  }

  @Test
  void ensureBioIsSet() {
    final var bio = new Bio("Some bio");
    assertEquals("Some bio", bio.toString());
  }

  @Test
  void ensureBioIsChanged() {
    final var bio = new Bio("Some bio");
    bio.setBio("Some other bio");
    assertEquals("Some other bio", bio.toString());
  }

  @Test
  void ensureInvalidChangeKeepsPreviousBio() {
    final var bio = new Bio("Some bio");

    assertThrows(IllegalArgumentException.class, () -> bio.setBio(""));
    assertEquals("Some bio", bio.toString());
  }
}
