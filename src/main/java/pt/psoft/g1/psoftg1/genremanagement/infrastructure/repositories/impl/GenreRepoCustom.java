package pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.impl;

import java.time.LocalDate;
import java.util.List;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDto;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDto;

interface GenreRepoCustom {
  List<GenreLendingsPerMonthDto> getLendingsPerMonthLastYearByGenre();

  List<GenreLendingsDto> getAverageLendingsInMonth(
      LocalDate month, pt.psoft.g1.psoftg1.shared.services.Page page);

  List<GenreLendingsPerMonthDto> getLendingsAverageDurationPerMonth(
      LocalDate startDate, LocalDate endDate);
}
