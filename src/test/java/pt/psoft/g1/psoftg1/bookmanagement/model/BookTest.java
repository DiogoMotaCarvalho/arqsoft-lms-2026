package pt.psoft.g1.psoftg1.bookmanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

class BookTest {
  private final String validIsbn = "9782826012092";
  private final String validTitle = "Encantos de contar";
  private final Author validAuthor1 =
      new Author(
          "João Alberto",
          "O João Alberto nasceu em Chaves e foi pedreiro a maior parte da sua vida.",
          null);
  private final Author validAuthor2 =
      new Author(
          "Maria José",
          "A Maria José nasceu em Viseu e só come laranjas às segundas feiras.",
          null);
  private final Genre validGenre = new Genre("Fantasia");
  private ArrayList<Author> authors = new ArrayList<>();

  @BeforeEach
  void setUp() {
    authors.clear();
  }

  @Test
  void ensureIsbnNotNull() {
    authors.add(validAuthor1);
    assertThrows(
        IllegalArgumentException.class,
        () -> new Book(null, validTitle, null, validGenre, authors, null));
  }

  @Test
  void ensureTitleNotNull() {
    authors.add(validAuthor1);
    assertThrows(
        IllegalArgumentException.class,
        () -> new Book(validIsbn, null, null, validGenre, authors, null));
  }

  @Test
  void ensureGenreNotNull() {
    authors.add(validAuthor1);
    assertThrows(
        IllegalArgumentException.class,
        () -> new Book(validIsbn, validTitle, null, null, authors, null));
  }

  @Test
  void ensureAuthorsNotNull() {
    authors.add(validAuthor1);
    assertThrows(
        IllegalArgumentException.class,
        () -> new Book(validIsbn, validTitle, null, validGenre, null, null));
  }

  @Test
  void ensureAuthorsNotEmpty() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Book(validIsbn, validTitle, null, validGenre, authors, null));
  }

  @Test
  void ensureBookCreatedWithMultipleAuthors() {
    authors.add(validAuthor1);
    authors.add(validAuthor2);
    assertDoesNotThrow(() -> new Book(validIsbn, validTitle, null, validGenre, authors, null));
  }

  private Book persistedBook(String photoUri) {
    authors.add(validAuthor1);
    Book book = new Book(validIsbn, validTitle, "Descrição", validGenre, authors, photoUri);
    ReflectionTestUtils.setField(book, "version", 3L);
    return book;
  }

  @Test
  void ensureGettersReturnConstructorValues() {
    Book book = persistedBook("cover.png");

    assertEquals(validIsbn, book.getIsbn());
    assertEquals(validTitle, book.getTitle().toString());
    assertEquals("Descrição", book.getDescription());
    assertEquals(validGenre, book.getGenre());
    assertEquals(List.of(validAuthor1), book.getAuthors());
    assertEquals("cover.png", book.getPhoto().getPhotoFile());
  }

  @Test
  void ensurePatchWithStaleVersionFails() {
    Book book = persistedBook(null);
    assertThrows(
        StaleObjectStateException.class,
        () -> book.applyPatch(2L, "Outro", null, null, null, null));
    assertEquals(validTitle, book.getTitle().toString());
  }

  @Test
  void ensurePatchUpdatesProvidedFields() {
    Book book = persistedBook(null);
    Genre terror = new Genre("Terror");

    book.applyPatch(3L, "Novo titulo", "Nova descrição", terror, List.of(validAuthor2), "new.png");

    assertEquals("Novo titulo", book.getTitle().toString());
    assertEquals("Nova descrição", book.getDescription());
    assertEquals(terror, book.getGenre());
    assertEquals(List.of(validAuthor2), book.getAuthors());
    assertEquals("new.png", book.getPhoto().getPhotoFile());
  }

  @Test
  void ensurePatchKeepsFieldsThatAreNotProvided() {
    Book book = persistedBook("cover.png");

    book.applyPatch(3L, null, null, null, null, null);

    assertEquals(validTitle, book.getTitle().toString());
    assertEquals("Descrição", book.getDescription());
    assertEquals(validGenre, book.getGenre());
    assertEquals(List.of(validAuthor1), book.getAuthors());
    assertEquals("cover.png", book.getPhoto().getPhotoFile());
  }

  @Test
  void ensureRemovePhotoWithWrongVersionFails() {
    Book book = persistedBook("cover.png");

    assertThrows(ConflictException.class, () -> book.removePhoto(1L));
    assertNotNull(book.getPhoto());
  }

  @Test
  void ensureRemovePhotoClearsPhoto() {
    Book book = persistedBook("cover.png");

    book.removePhoto(3L);

    assertNull(book.getPhoto());
  }

  @Test
  void ensureBookWithInvalidIsbnIsRejected() {
    authors.add(validAuthor1);
    assertThrows(
        IllegalArgumentException.class,
        () -> new Book("9782826012099", validTitle, null, validGenre, authors, null));
  }

  @Test
  void ensureBookWithBlankTitleIsRejected() {
    authors.add(validAuthor1);
    assertThrows(
        IllegalArgumentException.class,
        () -> new Book(validIsbn, " ", null, validGenre, authors, null));
  }

  @Test
  void ensureNewBookHasNoVersion() {
    authors.add(validAuthor1);
    assertNull(new Book(validIsbn, validTitle, null, validGenre, authors, null).getVersion());
  }

  @Test
  void ensureBookDescriptionIsSanitized() {
    authors.add(validAuthor1);
    Book book =
        new Book(validIsbn, validTitle, "Livro<script>x()</script>", validGenre, authors, null);

    assertEquals("Livro", book.getDescription());
  }

  @Test
  void ensurePatchWithInvalidTitleFailsAndKeepsTitle() {
    Book book = persistedBook(null);
    assertThrows(
        IllegalArgumentException.class, () -> book.applyPatch(3L, " ", null, null, null, null));
    assertEquals(validTitle, book.getTitle().toString());
  }

  @Test
  void ensureRemovePhotoOnBookWithoutPhotoKeepsItEmpty() {
    Book book = persistedBook(null);

    book.removePhoto(3L);

    assertNull(book.getPhoto());
  }
}
