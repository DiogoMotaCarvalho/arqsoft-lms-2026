package pt.psoft.g1.psoftg1.genremanagement.services;

import java.util.List;
import java.util.Optional;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDto;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.shared.services.Page;

public interface GenreService {
  Iterable<Genre> findAll();

  Genre save(Genre genre);

  Optional<Genre> findByString(String name);

  List<GenreBookCountDto> findTopGenreByBooks();

  List<GenreLendingsDto> getAverageLendings(GetAverageLendingsQuery query, Page page);

  List<GenreLendingsPerMonthDto> getLendingsPerMonthLastYearByGenre();

  List<GenreLendingsPerMonthDto> getLendingsAverageDurationPerMonth(
      String startDate, String endDate);
}
