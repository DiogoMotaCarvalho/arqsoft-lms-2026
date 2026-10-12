package pt.psoft.g1.psoftg1.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
public class ForbiddenName {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long pk;

  @Getter
  @Setter
  @Column(nullable = false)
  @Size(min = 1)
  private String forbiddenName;

  public ForbiddenName(String name) {
    this.forbiddenName = name;
  }
}
