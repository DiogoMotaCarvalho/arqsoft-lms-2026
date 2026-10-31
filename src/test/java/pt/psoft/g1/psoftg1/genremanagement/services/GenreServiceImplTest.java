package pt.psoft.g1.psoftg1.genremanagement.services;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.genremanagement.repositories.InMemoryGenreRepository;

class GenreServiceImplTest {
  private final GenreServiceImpl service = new GenreServiceImpl(new InMemoryGenreRepository());

  @Test
  void ensureAverageDurationPeriodMustUseIsoDates() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.getLendingsAverageDurationPerMonth("01-01-2024", "2024-12-31"));
    assertThrows(
        IllegalArgumentException.class,
        () -> service.getLendingsAverageDurationPerMonth("2024-01-01", "31/12/2024"));
  }

  @Test
  void ensureAverageDurationPeriodCannotEndBeforeItStarts() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.getLendingsAverageDurationPerMonth("2024-06-01", "2024-05-31"));
  }
}
