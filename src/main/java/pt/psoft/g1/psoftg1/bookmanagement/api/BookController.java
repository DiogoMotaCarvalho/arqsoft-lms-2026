package pt.psoft.g1.psoftg1.bookmanagement.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicData;
import pt.psoft.g1.psoftg1.bookmanagement.services.BibliographicEnrichmentService;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookService;
import pt.psoft.g1.psoftg1.bookmanagement.services.CreateBookRequest;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;
import pt.psoft.g1.psoftg1.bookmanagement.services.UpdateBookRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.lendingmanagement.services.LendingService;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderService;
import pt.psoft.g1.psoftg1.shared.api.ListResponse;
import pt.psoft.g1.psoftg1.shared.services.ConcurrencyService;
import pt.psoft.g1.psoftg1.shared.services.FileStorageService;
import pt.psoft.g1.psoftg1.shared.services.SearchRequest;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.UserService;

@Tag(name = "Books", description = "Endpoints for managing Books")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class BookController {
  private final BookService bookService;
  private final LendingService lendingService;
  private final ConcurrencyService concurrencyService;
  private final FileStorageService fileStorageService;
  private final UserService userService;
  private final ReaderService readerService;
  private final BibliographicEnrichmentService enrichmentService;

  private final BookViewMapper bookViewMapper;

  @Operation(summary = "Register a new Book")
  @PutMapping(value = "/{isbn}")
  @ResponseStatus(HttpStatus.CREATED)
  public ResponseEntity<BookView> create(
      CreateBookRequest resource, @PathVariable("isbn") String isbn) {

    // Guarantee that the client doesn't provide a link on the body, null = no photo or error
    resource.setPhotoURI(null);
    MultipartFile file = resource.getPhoto();

    String fileName = fileStorageService.getRequestPhoto(file);

    if (fileName != null) {
      resource.setPhotoURI(fileName);
    }

    Book book;
    try {
      book = bookService.create(resource, isbn);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    // final var savedBook = bookService.save(book);
    final var newBookUri =
        ServletUriComponentsBuilder.fromCurrentRequestUri()
            .pathSegment(book.getIsbn())
            .build()
            .toUri();

    return ResponseEntity.created(newBookUri)
        .eTag(Long.toString(book.getVersion()))
        .body(bookViewMapper.toBookView(book));
  }

  @Operation(summary = "Gets a specific Book by isbn")
  @GetMapping(value = "/{isbn}")
  public ResponseEntity<BookView> findByIsbn(@PathVariable final String isbn) {

    final var book = bookService.findByIsbn(isbn);

    BookView bookView = bookViewMapper.toBookView(book);
    // Enrich only when there is a gap to fill (#6): a complete local record makes no external call,
    // so the external dependency never touches a read it cannot improve. Best-effort, local-wins.
    if (bookView.getDescription() == null || bookView.getDescription().isBlank()) {
      enrichmentService.enrichmentFor(isbn).ifPresent(data -> fillGaps(bookView, data));
    }

    return ResponseEntity.ok().eTag(Long.toString(book.getVersion())).body(bookView);
  }

  /**
   * Fills only the gaps in the view from the external data. Local values always win, and the title,
   * ISBN, genre, and authors are never touched (#6, ADR-0013). In practice this fills the
   * description when the local one is blank and the external one is present: the authors are not
   * gap-filled because a {@code Book} is required to have at least one author, so a retrieved
   * book's author list is never empty.
   */
  private static void fillGaps(final BookView view, final BibliographicData data) {
    if ((view.getDescription() == null || view.getDescription().isBlank())
        && data.getDescription() != null
        && !data.getDescription().isBlank()) {
      view.setDescription(data.getDescription());
    }
  }

  @Operation(summary = "Deletes a book photo")
  @DeleteMapping("/{isbn}/photo")
  public ResponseEntity<Void> deleteBookPhoto(@PathVariable("isbn") final String isbn) {

    var book = bookService.findByIsbn(isbn);
    if (book.getPhoto() == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    fileStorageService.deleteFile(book.getPhoto().getPhotoFile());
    bookService.removeBookPhoto(book.getIsbn(), book.getVersion());

    return ResponseEntity.ok().build();
  }

  @Operation(summary = "Gets a book photo")
  @GetMapping("/{isbn}/photo")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<byte[]> getSpecificBookPhoto(@PathVariable("isbn") final String isbn) {

    Book book = bookService.findByIsbn(isbn);

    // In case the user has no photo, just return a 200 OK without body
    if (book.getPhoto() == null) {
      return ResponseEntity.ok().build();
    }

    String photoFile = book.getPhoto().getPhotoFile();
    byte[] image = fileStorageService.getFile(photoFile);
    String fileFormat =
        fileStorageService
            .getExtension(book.getPhoto().getPhotoFile())
            .orElseThrow(() -> new ValidationException("Unable to get file extension"));

    if (image == null) {
      return ResponseEntity.ok().build();
    }

    return ResponseEntity.ok()
        .contentType(fileFormat.equals("png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
        .body(image);
  }

  @Operation(summary = "Updates a specific Book")
  @PatchMapping(value = "/{isbn}")
  public ResponseEntity<BookView> updateBook(
      @PathVariable final String isbn,
      final WebRequest request,
      @Valid final UpdateBookRequest resource) {

    final String ifMatchValue = request.getHeader(ConcurrencyService.IF_MATCH);
    if (ifMatchValue == null || ifMatchValue.isEmpty() || ifMatchValue.equals("null")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "You must issue a conditional PATCH using 'if-match'");
    }

    MultipartFile file = resource.getPhoto();

    String fileName = fileStorageService.getRequestPhoto(file);

    if (fileName != null) {
      resource.setPhotoURI(fileName);
    }

    Book book;
    resource.setIsbn(isbn);
    try {
      book =
          bookService.update(
              resource,
              String.valueOf(concurrencyService.getVersionFromIfMatchHeader(ifMatchValue)));
    } catch (Exception e) {
      throw new ConflictException("Could not update book: " + e.getMessage());
    }
    return ResponseEntity.ok()
        .eTag(Long.toString(book.getVersion()))
        .body(bookViewMapper.toBookView(book));
  }

  @Operation(summary = "Gets Books by title or genre")
  @GetMapping
  public ListResponse<BookView> findBooks(
      @RequestParam(value = "title", required = false) final String title,
      @RequestParam(value = "genre", required = false) final String genre,
      @RequestParam(value = "authorName", required = false) final String authorName) {

    // Para uma junção 'AND', ver o "/search"

    List<Book> booksByTitle = null;
    if (title != null) {
      booksByTitle = bookService.findByTitle(title);
    }

    List<Book> booksByGenre = null;
    if (genre != null) {
      booksByGenre = bookService.findByGenre(genre);
    }

    List<Book> booksByAuthorName = null;
    if (authorName != null) {
      booksByAuthorName = bookService.findByAuthorName(authorName);
    }

    Set<Book> bookSet = new HashSet<>();
    if (booksByTitle != null) {
      bookSet.addAll(booksByTitle);
    }
    if (booksByGenre != null) {
      bookSet.addAll(booksByGenre);
    }
    if (booksByAuthorName != null) {
      bookSet.addAll(booksByAuthorName);
    }

    List<Book> books =
        bookSet.stream()
            .sorted(Comparator.comparing(b -> b.getTitle().toString()))
            .collect(Collectors.toList());

    if (books.isEmpty()) {
      throw new NotFoundException("No books found with the provided criteria");
    }

    return new ListResponse<>(bookViewMapper.toBookView(books));
  }

  @Operation(summary = "Gets the top 5 books lent")
  @GetMapping("top5")
  public ListResponse<BookCountView> getTop5BooksLent() {
    return new ListResponse<>(bookViewMapper.toBookCountView(bookService.findTop5BooksLent()));
  }

  @Operation(summary = "Gets some books suggestions based on the reader's interests")
  @GetMapping("suggestions")
  public ListResponse<BookView> getBooksSuggestions(Authentication authentication) {
    User loggedUser = userService.getAuthenticatedUser(authentication);
    ReaderDetails readerDetails =
        readerService
            .findByUsername(loggedUser.getUsername())
            .orElseThrow(
                () -> new NotFoundException(ReaderDetails.class, loggedUser.getUsername()));

    return new ListResponse<>(
        bookViewMapper.toBookView(
            bookService.getBooksSuggestionsForReader(readerDetails.getReaderNumber())));
  }

  @Operation(summary = "Get average lendings duration")
  @GetMapping(value = "/{isbn}/avgDuration")
  public @ResponseBody ResponseEntity<BookAverageLendingDurationView> getAvgLendingDurationByIsbn(
      @PathVariable("isbn") final String isbn) {
    final var book = bookService.findByIsbn(isbn);
    Double avgDuration = lendingService.getAvgLendingDurationByIsbn(isbn);

    return ResponseEntity.ok()
        .body(bookViewMapper.toBookAverageLendingDurationView(book, avgDuration));
  }

  @PostMapping("/search")
  public ListResponse<BookView> searchBooks(
      @RequestBody final SearchRequest<SearchBooksQuery> request) {
    final var bookList = bookService.searchBooks(request.getPage(), request.getQuery());
    return new ListResponse<>(bookViewMapper.toBookView(bookList));
  }
}
