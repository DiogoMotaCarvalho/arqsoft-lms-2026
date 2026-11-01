package pt.psoft.g1.psoftg1.readermanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ReaderNumberTest {
  @Test
  void ensureReaderNumberWithYearAndNumber() {
    assertEquals("2023/42", new ReaderNumber(2023, 42).toString());
  }

  @Test
  void ensureReaderNumberDefaultsToCurrentYear() {
    assertEquals(LocalDate.now().getYear() + "/7", new ReaderNumber(7).toString());
  }
}
