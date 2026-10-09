package pt.psoft.g1.psoftg1.genremanagement.services;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GenreLendingsPerMonthDTO {
  private int year;
  private int month;
  List<GenreLendingsDTO> values;
}
