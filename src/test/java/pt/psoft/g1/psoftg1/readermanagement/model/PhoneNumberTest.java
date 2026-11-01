package pt.psoft.g1.psoftg1.readermanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {
  @ParameterizedTest
  @ValueSource(strings = {"912345678", "212345678"})
  void ensureMobileAndFixedNumbersAreAccepted(String number) {
    assertEquals(number, new PhoneNumber(number).toString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "91234567", "21234567", "9123456789", "00123456789"})
  void ensureNumberMustHaveNineDigits(String number) {
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber(number));
  }

  @ParameterizedTest
  @ValueSource(strings = {"112345678", "312345678", "512345678", "812345678", "+35191234"})
  void ensureNumberMustStartWith9Or2(String number) {
    assertThrows(IllegalArgumentException.class, () -> new PhoneNumber(number));
  }
}
