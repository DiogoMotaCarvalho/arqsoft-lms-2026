package pt.psoft.g1.psoftg1.bookmanagement.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.InMemoryAuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.InMemoryBookRepository;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.InMemoryGenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.InMemoryReaderRepository;
import pt.psoft.g1.psoftg1.shared.repositories.InMemoryPhotoRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

class BookServiceImplTest {
  private static final long SUGGESTIONS_PER_GENRE = 2;
  private static final String ISBN = "9782826012092";
  private static final String OTHER_ISBN = "8175257660";

  private final InMemoryBookRepository bookRepository = new InMemoryBookRepository();
  private final InMemoryGenreRepository genreRepository = new InMemoryGenreRepository();
  private final InMemoryAuthorRepository authorRepository = new InMemoryAuthorRepository();
  private final InMemoryPhotoRepository photoRepository = new InMemoryPhotoRepository();
  private final InMemoryReaderRepository readerRepository = new InMemoryReaderRepository();

  private final BookServiceImpl service =
      new BookServiceImpl(
          bookRepository, genreRepository, authorRepository, photoRepository, readerRepository);

  private Genre fantasy;
  private Genre horror;
  private Author author;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(service, "suggestionsLimitPerGenre", SUGGESTIONS_PER_GENRE);
    fantasy = genreRepository.save(new Genre("Fantasia"));
    horror = genreRepository.save(new Genre("Terror"));
    author = authorRepository.save(new Author("Maria José", "Escritora portuguesa", null));
  }

  private CreateBookRequest newBook(String genre, List<Long> authors) {
    CreateBookRequest request = new CreateBookRequest();
    request.setTitle("Encantos de contar");
    request.setGenre(genre);
    request.setAuthors(authors);
    return request;
  }

  private Book existingBook(String isbn, Genre genre, String photoUri) {
    return bookRepository.save(
        new Book(isbn, "Livro " + isbn, null, genre, List.of(author), photoUri));
  }

  private UpdateBookRequest changeTitle(Book book, String title) {
    return new UpdateBookRequest(
        book.getIsbn(), title, null, List.of(author.getAuthorNumber()), null);
  }

  private ReaderDetails readerInterestedIn(List<Genre> interests) {
    return readerRepository.save(
        new ReaderDetails(
            1,
            Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Coives"),
            "2000-01-01",
            "919191919",
            true,
            false,
            false,
            null,
            interests));
  }

  @Test
  void ensureBookIsRegistered() {
    Book created = service.create(newBook("Fantasia", List.of(author.getAuthorNumber())), ISBN);

    assertEquals(ISBN, created.getIsbn());
    assertEquals(fantasy, created.getGenre());
    assertEquals(List.of(author), created.getAuthors());
    assertEquals(List.of(created), bookRepository.findAll());
  }

  @Test
  void ensureIsbnMustBeUnique() {
    Book existing = existingBook(ISBN, fantasy, null);
    CreateBookRequest request = newBook("Fantasia", List.of(author.getAuthorNumber()));

    assertThrows(ConflictException.class, () -> service.create(request, ISBN));
    assertEquals(List.of(existing), bookRepository.findAll());
  }

  @Test
  void ensureGenreMustExist() {
    CreateBookRequest request = newBook("Inexistente", List.of(author.getAuthorNumber()));

    assertThrows(NotFoundException.class, () -> service.create(request, ISBN));
    assertTrue(bookRepository.findAll().isEmpty());
  }

  @Test
  void ensureBookNeedsAtLeastOneExistingAuthor() {
    CreateBookRequest request = newBook("Fantasia", List.of(999L));

    assertThrows(IllegalArgumentException.class, () -> service.create(request, ISBN));
    assertTrue(bookRepository.findAll().isEmpty());
  }

  @Test
  void ensurePhotoIsStoredWhenFileAndUriAreProvided() {
    CreateBookRequest request = newBook("Fantasia", List.of(author.getAuthorNumber()));
    request.setPhoto(new MockMultipartFile("photo", "cover.png", "image/png", new byte[] {1}));
    request.setPhotoURI("cover.png");

    Book created = service.create(request, ISBN);

    assertEquals("cover.png", created.getPhoto().getPhotoFile());
  }

  @Test
  void ensureUpdateChangesTitleAndGenre() {
    Book book = existingBook(ISBN, fantasy, null);
    UpdateBookRequest request =
        new UpdateBookRequest(
            ISBN, "Novo titulo", "Terror", List.of(author.getAuthorNumber()), null);

    service.update(request, book.getVersion().toString());

    assertEquals("Novo titulo", book.getTitle().toString());
    assertEquals(horror, book.getGenre());
  }

  @Test
  void ensureCannotUpdateUnknownBook() {
    Book notSaved = new Book(ISBN, "Fantasma", null, fantasy, List.of(author), null);
    UpdateBookRequest request = changeTitle(notSaved, "Novo titulo");

    assertThrows(NotFoundException.class, () -> service.update(request, "0"));
  }

  @Test
  void ensureCannotUpdateWithStaleVersion() {
    Book book = existingBook(ISBN, fantasy, null);
    UpdateBookRequest request = changeTitle(book, "Novo titulo");

    assertThrows(StaleObjectStateException.class, () -> service.update(request, "7"));
    assertEquals("Livro " + ISBN, book.getTitle().toString());
  }

  @Test
  void ensureUpdateRejectsUnknownGenre() {
    Book book = existingBook(ISBN, fantasy, null);
    UpdateBookRequest request =
        new UpdateBookRequest(ISBN, null, "Inexistente", List.of(author.getAuthorNumber()), null);
    String version = book.getVersion().toString();

    assertThrows(NotFoundException.class, () -> service.update(request, version));
    assertEquals(fantasy, book.getGenre());
  }

  @Test
  void ensureRemovingPhotoAlsoDeletesTheFile() {
    Book book = existingBook(ISBN, fantasy, "cover.png");

    service.removeBookPhoto(ISBN, book.getVersion());

    assertNull(book.getPhoto());
    assertEquals(List.of("cover.png"), photoRepository.deletedPhotoFiles());
  }

  @Test
  void ensureCannotRemovePhotoFromBookWithoutPhoto() {
    Book book = existingBook(ISBN, fantasy, null);
    long version = book.getVersion();

    assertThrows(NotFoundException.class, () -> service.removeBookPhoto(ISBN, version));
    assertTrue(photoRepository.deletedPhotoFiles().isEmpty());
  }

  @Test
  void ensureSuggestionsComeFromTheReadersInterests() {
    Book fantasyBook = existingBook(ISBN, fantasy, null);
    existingBook(OTHER_ISBN, horror, null);
    ReaderDetails reader = readerInterestedIn(List.of(fantasy));

    assertEquals(
        List.of(fantasyBook), service.getBooksSuggestionsForReader(reader.getReaderNumber()));
  }

  @Test
  void ensureSuggestionsAreLimitedPerGenre() {
    existingBook(ISBN, fantasy, null);
    existingBook(OTHER_ISBN, fantasy, null);
    existingBook("9780000000040", fantasy, null);
    Book horrorBook = existingBook("080442957X", horror, null);
    ReaderDetails reader = readerInterestedIn(List.of(fantasy, horror));

    List<Book> suggestions = service.getBooksSuggestionsForReader(reader.getReaderNumber());

    assertEquals(SUGGESTIONS_PER_GENRE + 1, suggestions.size());
    assertTrue(suggestions.contains(horrorBook));
  }

  @Test
  void ensureReaderWithoutInterestsGetsNoSuggestions() {
    ReaderDetails reader = readerInterestedIn(List.of());
    String readerNumber = reader.getReaderNumber();

    assertThrows(NotFoundException.class, () -> service.getBooksSuggestionsForReader(readerNumber));
  }

  @Test
  void ensureSuggestionsRequireAnExistingReader() {
    assertThrows(NotFoundException.class, () -> service.getBooksSuggestionsForReader("2024/999"));
  }
}
