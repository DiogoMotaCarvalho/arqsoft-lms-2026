package pt.psoft.g1.psoftg1.readermanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DateTimeException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.access.AccessDeniedException;

class BirthDateTest {

  @Test
  void ensureBirthDateCanBeCreatedWithValidDate() {
    assertEquals(LocalDate.of(2000, 1, 1), new BirthDate(2000, 1, 1).getBirthDate());
  }

  @Test
  void ensureBirthDateCanBeCreatedWithValidStringDate() {
    assertEquals(LocalDate.of(2000, 1, 31), new BirthDate("2000-01-31").getBirthDate());
  }

  @Test
  void ensureLeapDayIsAccepted() {
    assertEquals(LocalDate.of(2000, 2, 29), new BirthDate("2000-02-29").getBirthDate());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"01-01-2000", "2000/01/01", "2000-1-1", "20000101", "2000-01-01T00:00", ""})
  void ensureStringDateMustUseIsoFormat(String date) {
    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, () -> new BirthDate(date));
    assertEquals(
        "Provided birth date is not in a valid format. Use yyyy-MM-dd", exception.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"2000-13-01", "2000-00-10", "2001-02-29", "2000-04-31"})
  void ensureNonExistentDatesAreRejected(String date) {
    assertThrows(DateTimeException.class, () -> new BirthDate(date));
  }

  @Test
  void ensureBirthDateCannotBeInTheFuture() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);

    assertThrows(
        AccessDeniedException.class,
        () ->
            new BirthDate(tomorrow.getYear(), tomorrow.getMonthValue(), tomorrow.getDayOfMonth()));
  }

  @Test
  void ensureCorrectStringRepresentation() {
    BirthDate birthDate = new BirthDate(2000, 1, 1);
    assertEquals("2000-1-1", birthDate.toString());
  }
}
