package pt.psoft.g1.psoftg1.authormanagement.repositories;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorLendingView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;

public class InMemoryAuthorRepository implements AuthorRepository {
  private final List<Author> authors = new ArrayList<>();
  private long nextAuthorNumber = 1;

  @Override
  public Author save(Author author) {
    if (author.getAuthorNumber() == null) {
      ReflectionTestUtils.setField(author, "authorNumber", nextAuthorNumber++);
    }
    if (!authors.contains(author)) {
      authors.add(author);
    }
    return author;
  }

  @Override
  public Optional<Author> findByAuthorNumber(Long authorNumber) {
    return authors.stream()
        .filter(a -> Objects.equals(a.getAuthorNumber(), authorNumber))
        .findFirst();
  }

  @Override
  public Iterable<Author> findAll() {
    return List.copyOf(authors);
  }

  @Override
  public List<Author> searchByNameNameStartsWith(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Author> searchByNameName(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Page<AuthorLendingView> findTopAuthorByLendings(Pageable pageableRules) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(Author author) {
    authors.remove(author);
  }

  @Override
  public List<Author> findCoAuthorsByAuthorNumber(Long authorNumber) {
    throw new UnsupportedOperationException();
  }
}
