package pt.psoft.g1.psoftg1.genremanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GenreTest {
  private static final int MAX_LENGTH = 100;

  @Test
  void ensureGenreMustNotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new Genre(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\t"})
  void ensureGenreMustNotBeBlank(String genre) {
    assertThrows(IllegalArgumentException.class, () -> new Genre(genre));
  }

  @Test
  void ensureGenreAcceptsMaximumLength() {
    assertEquals(MAX_LENGTH, new Genre("a".repeat(MAX_LENGTH)).getGenre().length());
  }

  @Test
  void ensureGenreMustNotBeOversize() {
    assertThrows(IllegalArgumentException.class, () -> new Genre("a".repeat(MAX_LENGTH + 1)));
  }

  @Test
  void ensureGenreIsSet() {
    final var genre = new Genre("Some genre");
    assertEquals("Some genre", genre.toString());
    assertEquals("Some genre", genre.getGenre());
  }
}
