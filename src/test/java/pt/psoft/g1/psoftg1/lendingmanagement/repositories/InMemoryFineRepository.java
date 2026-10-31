package pt.psoft.g1.psoftg1.lendingmanagement.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;

public class InMemoryFineRepository implements FineRepository {
  private final List<Fine> fines = new ArrayList<>();

  @Override
  public Fine save(Fine fine) {
    if (!fines.contains(fine)) {
      fines.add(fine);
    }
    return fine;
  }

  @Override
  public Optional<Fine> findByLendingNumber(String lendingNumber) {
    return fines.stream()
        .filter(f -> f.getLending().getLendingNumber().equals(lendingNumber))
        .findFirst();
  }

  @Override
  public Iterable<Fine> findAll() {
    return List.copyOf(fines);
  }
}
