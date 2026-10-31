package pt.psoft.g1.psoftg1.bookmanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IsbnTest {

  @Test
  void ensureIsbnMustNotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn(null));
  }

  @Test
  void ensureIsbnMustNotBeBlank() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn(""));
  }

  @ParameterizedTest
  @ValueSource(strings = {"978282601209", "97828260120921", "817525766", "81752576600"})
  void ensureIsbnMustHave10Or13Digits(String isbn) {
    assertThrows(IllegalArgumentException.class, () -> new Isbn(isbn));
  }

  @ParameterizedTest
  @ValueSource(strings = {"978-2826012092", "978282601209X", "978282601209a", "81752576X0"})
  void ensureIsbnMustOnlyContainDigits(String isbn) {
    assertThrows(IllegalArgumentException.class, () -> new Isbn(isbn));
  }

  @Test
  void ensureIsbn13IsSet() {
    final var isbn = new Isbn("9782826012092");
    assertEquals("9782826012092", isbn.toString());
  }

  @Test
  void ensureChecksum13IsCorrect() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn("9782826012099"));
  }

  @Test
  void ensureIsbn13AcceptsZeroCheckDigit() {
    assertDoesNotThrow(() -> new Isbn("9780000000040"));
  }

  @Test
  void ensureIsbn10IsSet() {
    final var isbn = new Isbn("8175257660");
    assertEquals("8175257660", isbn.toString());
  }

  @Test
  void ensureChecksum10IsCorrect() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn("8175257667"));
  }

  @Test
  void ensureIsbn10AcceptsXAsCheckDigit() {
    assertEquals("080442957X", new Isbn("080442957X").toString());
  }

  @Test
  void ensureIsbn10RejectsLowercaseXAsCheckDigit() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn("080442957x"));
  }

  @Test
  void ensureIsbnsWithSameValueAreEqual() {
    assertEquals(new Isbn("9782826012092"), new Isbn("9782826012092"));
    assertEquals(new Isbn("9782826012092").hashCode(), new Isbn("9782826012092").hashCode());
    assertNotEquals(new Isbn("9782826012092"), new Isbn("8175257660"));
  }
}
