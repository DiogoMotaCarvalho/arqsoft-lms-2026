package pt.psoft.g1.psoftg1.genremanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Schema(description = "A Genre and its lendings count per month.")
@AllArgsConstructor
public class GenreLendingsCountPerMonthView {
  private Integer year;
  private Integer month;
  private List<GenreLendingsView> lendingsCount;
}
