package pt.psoft.g1.psoftg1.lendingmanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.io.Serializable;
import lombok.Data;

/** DTO for {@link pt.psoft.g1.psoftg1.lendingmanagement.model.Fine} */
@Data
@Schema(description = "A Fine")
public class FineView implements Serializable {
  @PositiveOrZero private int centsValue;

  @NotNull private LendingView lending;
}
