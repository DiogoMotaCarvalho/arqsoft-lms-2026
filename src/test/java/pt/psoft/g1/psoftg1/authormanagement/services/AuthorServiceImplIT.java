package pt.psoft.g1.psoftg1.authormanagement.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;

@SpringBootTest
@Transactional
class AuthorServiceImplIT {
  @Autowired private AuthorService authorService;
  @Autowired private AuthorRepository authorRepository;

  @Test
  void findsAStoredAuthorByItsAuthorNumber() {
    Author stored = authorRepository.save(new Author("Alex", "O Alex escreveu livros", null));

    Optional<Author> found = authorService.findByAuthorNumber(stored.getAuthorNumber());

    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Alex");
  }

  @Test
  void findsNothingForAnUnknownAuthorNumber() {
    Author stored = authorRepository.save(new Author("Alex", "O Alex escreveu livros", null));

    assertThat(authorService.findByAuthorNumber(stored.getAuthorNumber() + 1000)).isEmpty();
  }
}
