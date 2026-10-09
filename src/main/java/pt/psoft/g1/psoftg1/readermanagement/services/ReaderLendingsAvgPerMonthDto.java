package pt.psoft.g1.psoftg1.readermanagement.services;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Schema(description = "A Genre and its lending duration averages per month.")
@AllArgsConstructor
public class ReaderLendingsAvgPerMonthDto {
  private Integer year;
  private Integer month;
  private List<ReaderAverageDto> durationAverages;
}
