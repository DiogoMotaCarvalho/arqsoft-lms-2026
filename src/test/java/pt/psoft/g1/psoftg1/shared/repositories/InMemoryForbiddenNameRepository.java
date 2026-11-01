package pt.psoft.g1.psoftg1.shared.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;

public class InMemoryForbiddenNameRepository implements ForbiddenNameRepository {
  private final List<ForbiddenName> names = new ArrayList<>();

  @Override
  public ForbiddenName save(ForbiddenName forbiddenName) {
    if (!names.contains(forbiddenName)) {
      names.add(forbiddenName);
    }
    return forbiddenName;
  }

  @Override
  public List<ForbiddenName> findByForbiddenNameIsContained(String pat) {
    return names.stream().filter(n -> pat.contains(n.getForbiddenName())).toList();
  }

  @Override
  public Optional<ForbiddenName> findByForbiddenName(String forbiddenName) {
    return names.stream().filter(n -> n.getForbiddenName().equals(forbiddenName)).findFirst();
  }

  @Override
  public Iterable<ForbiddenName> findAll() {
    return List.copyOf(names);
  }

  @Override
  public int deleteForbiddenName(String forbiddenName) {
    int before = names.size();
    names.removeIf(n -> n.getForbiddenName().equals(forbiddenName));
    return before - names.size();
  }
}
