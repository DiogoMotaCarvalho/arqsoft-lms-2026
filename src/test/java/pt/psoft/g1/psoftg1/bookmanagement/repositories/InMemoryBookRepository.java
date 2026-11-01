package pt.psoft.g1.psoftg1.bookmanagement.repositories;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookCountDto;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;

public class InMemoryBookRepository implements BookRepository {
  private final List<Book> books = new ArrayList<>();

  @Override
  public Book save(Book book) {
    if (book.getVersion() == null) {
      ReflectionTestUtils.setField(book, "version", 0L);
    }
    if (!books.contains(book)) {
      books.add(book);
    }
    return book;
  }

  @Override
  public Optional<Book> findByIsbn(String isbn) {
    return books.stream().filter(b -> b.getIsbn().equals(isbn)).findFirst();
  }

  @Override
  public List<Book> findByGenre(String genre) {
    return books.stream().filter(b -> b.getGenre().toString().contains(genre)).toList();
  }

  public List<Book> findAll() {
    return List.copyOf(books);
  }

  @Override
  public List<Book> findByTitle(String title) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Book> findByAuthorName(String authorName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Page<BookCountDto> findTop5BooksLent(LocalDate oneYearAgo, Pageable pageable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Book> findBooksByAuthorNumber(Long authorNumber) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<Book> searchBooks(
      pt.psoft.g1.psoftg1.shared.services.Page page, SearchBooksQuery query) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(Book book) {
    books.remove(book);
  }
}
