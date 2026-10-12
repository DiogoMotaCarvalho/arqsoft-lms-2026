package pt.psoft.g1.psoftg1.genremanagement.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDto;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDto;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDto;

public interface GenreRepository {

  Iterable<Genre> findAll();

  Optional<Genre> findByString(String genreName);

  Genre save(Genre genre);

  Page<GenreBookCountDto> findTop5GenreByBookCount(Pageable pageable);

  List<GenreLendingsDto> getAverageLendingsInMonth(
      LocalDate month, pt.psoft.g1.psoftg1.shared.services.Page page);

  List<GenreLendingsPerMonthDto> getLendingsPerMonthLastYearByGenre();

  List<GenreLendingsPerMonthDto> getLendingsAverageDurationPerMonth(
      LocalDate startDate, LocalDate endDate);

  void delete(Genre genre);
}
