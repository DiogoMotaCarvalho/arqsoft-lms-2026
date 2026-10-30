package pt.psoft.g1.psoftg1.usermanagement.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.SearchUsersQuery;

public class InMemoryUserRepository implements UserRepository {
  private final List<User> users = new ArrayList<>();
  private long nextId = 1;

  @Override
  public <S extends User> S save(S user) {
    if (user.getId() == null) {
      ReflectionTestUtils.setField(user, User.class, "id", nextId++, Long.class);
    }
    if (!users.contains(user)) {
      users.add(user);
    }
    return user;
  }

  @Override
  public <S extends User> List<S> saveAll(Iterable<S> entities) {
    List<S> saved = new ArrayList<>();
    entities.forEach(user -> saved.add(save(user)));
    return saved;
  }

  @Override
  public Optional<User> findById(Long id) {
    return users.stream().filter(u -> Objects.equals(u.getId(), id)).findFirst();
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return users.stream().filter(u -> u.getUsername().equals(username)).findFirst();
  }

  public List<User> findAll() {
    return List.copyOf(users);
  }

  @Override
  public List<User> searchUsers(Page page, SearchUsersQuery query) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<User> findByNameName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<User> findByNameNameContains(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(User user) {
    users.remove(user);
  }
}
