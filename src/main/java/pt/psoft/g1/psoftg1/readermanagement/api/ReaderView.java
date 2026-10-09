package pt.psoft.g1.psoftg1.readermanagement.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "A Reader")
public class ReaderView {
  private String readerNumber;
  private String email;
  private String fullName;
  private String birthDate;
  private String phoneNumber;
  private String photo;
  private boolean gdprConsent;
  private boolean marketingConsent;
  private boolean thirdPartySharingConsent;
  private List<String> interestList;
}
