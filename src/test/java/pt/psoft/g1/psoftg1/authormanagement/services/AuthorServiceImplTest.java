package pt.psoft.g1.psoftg1.authormanagement.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.InMemoryAuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.InMemoryBookRepository;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.shared.repositories.InMemoryPhotoRepository;

class AuthorServiceImplTest {
  private final InMemoryAuthorRepository authorRepository = new InMemoryAuthorRepository();
  private final InMemoryPhotoRepository photoRepository = new InMemoryPhotoRepository();

  private final AuthorServiceImpl service =
      new AuthorServiceImpl(
          authorRepository, new InMemoryBookRepository(), new AuthorMapperImpl(), photoRepository);

  private static MockMultipartFile photoFile() {
    return new MockMultipartFile("photo", "joao.png", "image/png", new byte[] {1});
  }

  private Author existingAuthor(String photoUri) {
    return authorRepository.save(new Author("João Alberto", "Pedreiro e escritor", photoUri));
  }

  @Test
  void ensureAuthorIsRegistered() {
    Author created =
        service.create(new CreateAuthorRequest("João Alberto", "Pedreiro e escritor", null, null));

    assertEquals("João Alberto", created.getName());
    assertEquals("Pedreiro e escritor", created.getBio());
    assertNotNull(created.getAuthorNumber());
    assertEquals(List.of(created), authorRepository.findAll());
  }

  @Test
  void ensurePhotoIsStoredWhenFileAndUriAreProvided() {
    Author created =
        service.create(
            new CreateAuthorRequest("João Alberto", "Pedreiro", photoFile(), "joao.png"));

    assertEquals("joao.png", created.getPhoto().getPhotoFile());
  }

  @Test
  void ensurePhotoUriWithoutFileIsIgnored() {
    Author created =
        service.create(new CreateAuthorRequest("João Alberto", "Pedreiro", null, "joao.png"));

    assertNull(created.getPhoto());
  }

  @Test
  void ensurePhotoFileWithoutUriIsIgnored() {
    Author created =
        service.create(new CreateAuthorRequest("João Alberto", "Pedreiro", photoFile(), null));

    assertNull(created.getPhoto());
  }

  @Test
  void ensureUpdateChangesProvidedFields() {
    Author author = existingAuthor(null);

    service.partialUpdate(
        author.getAuthorNumber(),
        new UpdateAuthorRequest("Nova bio", "João Pedro", null, null),
        author.getVersion());

    assertEquals("João Pedro", author.getName());
    assertEquals("Nova bio", author.getBio());
  }

  @Test
  void ensureUpdateIgnoresPhotoUriWithoutFile() {
    Author author = existingAuthor("joao.png");

    service.partialUpdate(
        author.getAuthorNumber(),
        new UpdateAuthorRequest(null, null, null, "outra.png"),
        author.getVersion());

    assertEquals("joao.png", author.getPhoto().getPhotoFile());
  }

  @Test
  void ensureCannotUpdateUnknownAuthor() {
    UpdateAuthorRequest request = new UpdateAuthorRequest("Nova bio", null, null, null);

    assertThrows(NotFoundException.class, () -> service.partialUpdate(99L, request, 0));
  }

  @Test
  void ensureCannotUpdateWithStaleVersion() {
    Author author = existingAuthor(null);
    Long authorNumber = author.getAuthorNumber();
    UpdateAuthorRequest request = new UpdateAuthorRequest("Nova bio", null, null, null);

    assertThrows(
        StaleObjectStateException.class, () -> service.partialUpdate(authorNumber, request, 5));
    assertEquals("Pedreiro e escritor", author.getBio());
  }

  @Test
  void ensureRemovingPhotoAlsoDeletesTheFile() {
    Author author = existingAuthor("joao.png");

    service.removeAuthorPhoto(author.getAuthorNumber(), author.getVersion());

    assertNull(author.getPhoto());
    assertEquals(List.of("joao.png"), photoRepository.deletedPhotoFiles());
  }

  @Test
  void ensurePhotoIsKeptWhenRemovingWithStaleVersion() {
    Author author = existingAuthor("joao.png");
    Long authorNumber = author.getAuthorNumber();

    assertThrows(ConflictException.class, () -> service.removeAuthorPhoto(authorNumber, 5));
    assertNotNull(author.getPhoto());
    assertTrue(photoRepository.deletedPhotoFiles().isEmpty());
  }
}
