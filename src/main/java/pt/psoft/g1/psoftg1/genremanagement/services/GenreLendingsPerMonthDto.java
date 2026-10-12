package pt.psoft.g1.psoftg1.genremanagement.services;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenreLendingsPerMonthDto {
  private int year;
  private int month;
  List<GenreLendingsDto> values;
}
