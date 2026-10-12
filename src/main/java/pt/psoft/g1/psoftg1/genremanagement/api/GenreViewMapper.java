package pt.psoft.g1.psoftg1.genremanagement.api;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDto;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDto;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDto;
import pt.psoft.g1.psoftg1.shared.api.MapperInterface;

@Mapper(componentModel = "spring")
public abstract class GenreViewMapper extends MapperInterface {

  @Mapping(target = "genre", source = "genre")
  public abstract GenreView toGenreView(Genre genre);

  public abstract GenreView mapStringToGenreView(String genre);

  @Mapping(target = "genreView", source = "genre")
  public abstract GenreBookCountView toGenreBookCountView(GenreBookCountDto genreBookCountView);

  public abstract List<GenreBookCountView> toGenreBookCountView(
      List<GenreBookCountDto> genreBookCountView);

  public abstract GenreLendingsView toGenreAvgLendingsView(GenreLendingsDto dto);

  public abstract List<GenreLendingsView> toGenreAvgLendingsView(List<GenreLendingsDto> dtos);

  @Mapping(target = "lendingsCount", source = "values")
  public abstract GenreLendingsCountPerMonthView toGenreLendingsCountPerMonthView(
      GenreLendingsPerMonthDto dto);

  public abstract List<GenreLendingsCountPerMonthView> toGenreLendingsCountPerMonthView(
      List<GenreLendingsPerMonthDto> dtos);

  @Mapping(target = "durationAverages", source = "values")
  public abstract GenreLendingsAvgPerMonthView toGenreLendingsAveragePerMonthView(
      GenreLendingsPerMonthDto dto);

  public abstract List<GenreLendingsAvgPerMonthView> toGenreLendingsAveragePerMonthView(
      List<GenreLendingsPerMonthDto> dtos);
}
