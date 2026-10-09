package pt.psoft.g1.psoftg1.lendingmanagement.repositories;

import java.util.Optional;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;

public interface FineRepository {

  Optional<Fine> findByLendingNumber(String lendingNumber);

  Iterable<Fine> findAll();

  Fine save(Fine fine);
}
