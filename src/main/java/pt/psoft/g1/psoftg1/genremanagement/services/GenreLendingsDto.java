package pt.psoft.g1.psoftg1.genremanagement.services;

import jakarta.validation.constraints.NotNull;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Data;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

@Data
@AllArgsConstructor
public class GenreLendingsDto {
  @NotNull private String genre;
  private Number value;

  public GenreLendingsDto(String genre, Double value) {
    this.genre = genre;
    if (value == null) {
      this.value = 0;
    } else {
      this.value = formatValue(value);
    }
  }

  public GenreLendingsDto(String genre, Long value) {
    this.genre = genre;
    this.value = value;
  }

  public GenreLendingsDto(Genre genre, Double value) {
    this.genre = genre.toString();
    this.value = formatValue(value);
  }

  public GenreLendingsDto(Genre genre, Long value) {
    this.genre = genre.toString();
    this.value = value;
  }

  private Double formatValue(Double value) {
    return Double.valueOf(String.format(Locale.US, "%.1f", value));
  }
}
