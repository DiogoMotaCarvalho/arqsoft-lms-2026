package pt.psoft.g1.psoftg1.lendingmanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LendingNumberTest {
  private static final int CURRENT_YEAR = LocalDate.now().getYear();

  @Test
  void ensureLendingNumberNotNull() {
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(null));
  }

  @Test
  void ensureLendingNumberNotBlank() {
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(""));
  }

  @ParameterizedTest
  @ValueSource(strings = {"1/2024", "24/1", "2024-1", "2024\\1", "2024/", "2024/a", "abcd/1"})
  void ensureLendingNumberNotWrongFormat(String lendingNumber) {
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(lendingNumber));
  }

  @Test
  void ensureLendingNumberIsSetWithString() {
    assertEquals("2024/1", new LendingNumber("2024/1").toString());
  }

  @Test
  void ensureLeadingZerosInSequentialAreDropped() {
    assertEquals("2024/7", new LendingNumber("2024/007").toString());
  }

  @Test
  void ensureLendingNumberIsSetWithSequential() {
    assertEquals(CURRENT_YEAR + "/1", new LendingNumber(1).toString());
  }

  @Test
  void ensureLendingNumberIsSetWithYearAndSequential() {
    assertEquals("2024/1", new LendingNumber(2024, 1).toString());
  }

  @Test
  void ensureSequentialCanBeZero() {
    assertEquals("2024/0", new LendingNumber(2024, 0).toString());
    assertEquals(CURRENT_YEAR + "/0", new LendingNumber(0).toString());
  }

  @Test
  void ensureSequentialCannotBeNegative() {
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(2024, -1));
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(-1));
  }

  @Test
  void ensureYearCanBeTheCurrentYear() {
    assertEquals(CURRENT_YEAR + "/3", new LendingNumber(CURRENT_YEAR, 3).toString());
  }

  @Test
  void ensureYearCannotBeInTheFuture() {
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(CURRENT_YEAR + 1, 1));
  }

  @Test
  void ensureYearCannotBeBefore1970() {
    assertDoesNotThrow(() -> new LendingNumber(1970, 1));
    assertThrows(IllegalArgumentException.class, () -> new LendingNumber(1969, 1));
  }
}
