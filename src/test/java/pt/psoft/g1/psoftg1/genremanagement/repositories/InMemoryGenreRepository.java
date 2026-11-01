package pt.psoft.g1.psoftg1.genremanagement.repositories;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDto;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDto;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDto;

public class InMemoryGenreRepository implements GenreRepository {
  private final List<Genre> genres = new ArrayList<>();

  @Override
  public Genre save(Genre genre) {
    if (!genres.contains(genre)) {
      genres.add(genre);
    }
    return genre;
  }

  @Override
  public Optional<Genre> findByString(String genreName) {
    return genres.stream().filter(g -> g.getGenre().equals(genreName)).findFirst();
  }

  @Override
  public Iterable<Genre> findAll() {
    return List.copyOf(genres);
  }

  @Override
  public Page<GenreBookCountDto> findTop5GenreByBookCount(Pageable pageable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<GenreLendingsDto> getAverageLendingsInMonth(
      LocalDate month, pt.psoft.g1.psoftg1.shared.services.Page page) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<GenreLendingsPerMonthDto> getLendingsPerMonthLastYearByGenre() {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<GenreLendingsPerMonthDto> getLendingsAverageDurationPerMonth(
      LocalDate startDate, LocalDate endDate) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(Genre genre) {
    genres.remove(genre);
  }
}
