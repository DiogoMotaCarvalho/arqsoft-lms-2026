package pt.psoft.g1.psoftg1.bookmanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TitleTest {
  private static final int MAX_LENGTH = 128;

  @Test
  void ensureTitleMustNotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new Title(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\t\n"})
  void ensureTitleMustNotBeBlank(String title) {
    assertThrows(IllegalArgumentException.class, () -> new Title(title));
  }

  @Test
  void ensureTitleCantStartWithWhitespace() {
    final var title = new Title(" Some title");
    assertEquals("Some title", title.toString());
  }

  @Test
  void ensureTitleCantEndWithWhitespace() {
    final var title = new Title("Some title ");
    assertEquals("Some title", title.toString());
  }

  @Test
  void ensureTitleAcceptsMaximumLength() {
    final var title = new Title("a".repeat(MAX_LENGTH));
    assertEquals(MAX_LENGTH, title.toString().length());
  }

  @Test
  void ensureTitleMustNotBeOversize() {
    assertThrows(IllegalArgumentException.class, () -> new Title("a".repeat(MAX_LENGTH + 1)));
  }

  @Test
  void ensureTitleIsSet() {
    final var title = new Title("Some title");
    assertEquals("Some title", title.toString());
  }

  @Test
  void ensureTitleIsChanged() {
    final var title = new Title("Some title");
    title.setTitle("Some other title");
    assertEquals("Some other title", title.toString());
  }

  @Test
  void ensureInvalidChangeKeepsPreviousTitle() {
    final var title = new Title("Some title");

    assertThrows(IllegalArgumentException.class, () -> title.setTitle(" "));
    assertEquals("Some title", title.toString());
  }
}
