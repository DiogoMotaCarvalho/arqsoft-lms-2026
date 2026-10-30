package pt.psoft.g1.psoftg1.shared.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.InMemoryForbiddenNameRepository;

class ForbiddenNameServiceImplTest {
  private static final String NAMES_FILE = "forbiddenNamesTest.txt";

  private final InMemoryForbiddenNameRepository repository = new InMemoryForbiddenNameRepository();
  private final ForbiddenNameServiceImpl service = new ForbiddenNameServiceImpl(repository);

  private List<String> storedNames() {
    List<String> names = new ArrayList<>();
    repository.findAll().forEach(n -> names.add(n.getForbiddenName()));
    return names;
  }

  @Test
  void ensureNamesAreLoadedFromFile() {
    service.loadDataFromFile(NAMES_FILE);

    assertEquals(List.of("Batman", "Hulk", "Cinderela"), storedNames());
  }

  @Test
  void ensureAlreadyStoredNamesAreNotDuplicated() {
    repository.save(new ForbiddenName("Hulk"));

    service.loadDataFromFile(NAMES_FILE);

    assertEquals(List.of("Hulk", "Batman", "Cinderela"), storedNames());
  }

  @Test
  void ensureLoadingTwiceDoesNotDuplicateNames() {
    service.loadDataFromFile(NAMES_FILE);
    service.loadDataFromFile(NAMES_FILE);

    assertEquals(3, storedNames().size());
  }

  @Test
  void ensureMissingFileFails() {
    assertThrows(RuntimeException.class, () -> service.loadDataFromFile("missing.txt"));
  }
}
