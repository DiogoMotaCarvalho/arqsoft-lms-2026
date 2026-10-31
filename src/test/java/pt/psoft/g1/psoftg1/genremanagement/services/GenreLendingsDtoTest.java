package pt.psoft.g1.psoftg1.genremanagement.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

class GenreLendingsDtoTest {
  @Test
  void ensureDoubleValueIsRoundedToOneDecimal() {
    assertEquals(2.3, new GenreLendingsDto("Fantasia", 2.345).getValue());
    assertEquals(2.4, new GenreLendingsDto(new Genre("Fantasia"), 2.35).getValue());
  }

  @Test
  void ensureNullDoubleValueBecomesZero() {
    assertEquals(0, new GenreLendingsDto("Fantasia", (Double) null).getValue());
  }

  @Test
  void ensureLongValueIsKeptAsIs() {
    assertEquals(7L, new GenreLendingsDto("Fantasia", 7L).getValue());
    assertEquals(7L, new GenreLendingsDto(new Genre("Fantasia"), 7L).getValue());
  }

  @Test
  void ensureGenreNameIsTakenFromGenre() {
    assertEquals("Terror", new GenreLendingsDto(new Genre("Terror"), 1L).getGenre());
  }
}
