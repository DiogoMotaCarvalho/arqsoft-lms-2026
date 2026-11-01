package pt.psoft.g1.psoftg1.usermanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PasswordTest {
  @ParameterizedTest
  @ValueSource(strings = {"Manuelino123", "Abcdefg!", "aB345678", "Pass word"})
  void ensureValidPasswordsAreAccepted(String password) {
    assertDoesNotThrow(() -> new Password(password));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void ensureNullOrEmptyPasswordIsRejected(String password) {
    assertThrows(IllegalArgumentException.class, () -> new Password(password));
  }

  @Test
  void ensurePasswordNeedsAtLeastEightCharacters() {
    assertThrows(IllegalArgumentException.class, () -> new Password("Abcde1!"));
    assertDoesNotThrow(() -> new Password("Abcdef1!"));
  }

  @Test
  void ensurePasswordNeedsAnUpperCaseLetter() {
    assertThrows(IllegalArgumentException.class, () -> new Password("abcdefg1"));
  }

  @Test
  void ensurePasswordNeedsALowerCaseLetter() {
    assertThrows(IllegalArgumentException.class, () -> new Password("ABCDEFG1"));
  }

  @Test
  void ensurePasswordNeedsADigitOrSpecialCharacter() {
    assertThrows(IllegalArgumentException.class, () -> new Password("Abcdefgh"));
  }

  @Test
  void ensureUpdateWithInvalidPasswordFails() {
    Password password = new Password("Manuelino123");
    assertThrows(IllegalArgumentException.class, () -> password.updatePassword("weak"));
  }
}
