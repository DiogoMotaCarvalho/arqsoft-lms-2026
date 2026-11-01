package pt.psoft.g1.psoftg1.bookmanagement.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

/**
 * Component-level (C3.4) tests of bibliographic enrichment with the SUT equal to the controller,
 * the service, the repository, and the real bibliographic providers, with the external HTTP stubbed
 * by {@link StubbedBibliographicGateways} (#6, ADR-0013). The stub returns representative Google
 * Books and Open Library payloads of distinct models, so the real adapters and the selecting
 * provider run and only the network is faked. Each case asserts the externally observable outcome,
 * the description on the retrieved book's response.
 */
@Transactional
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Import(StubbedBibliographicGateways.class)
class BookEnrichmentComponentIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthorRepository authorRepository;
  @Autowired private GenreRepository genreRepository;
  @Autowired private BookRepository bookRepository;

  private Author author;
  private Genre genre;

  @BeforeEach
  void setUp() {
    author = new Author("Ana Maria Magalhaes", "A Portuguese author.", null);
    authorRepository.save(author);
    genre = new Genre("Genero");
    genreRepository.save(genre);
  }

  @Test
  void aBlankDescriptionIsFilledFromGoogleBooks() throws Exception {
    seed(StubbedBibliographicGateways.ISBN_ENRICHED, null);

    mockMvc
        .perform(get("/api/books/" + StubbedBibliographicGateways.ISBN_ENRICHED).with(reader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Enriched by Google Books"));
  }

  @Test
  void aBlankDescriptionIsFilledFromOpenLibraryWhenGoogleBooksHasNothing() throws Exception {
    // Google Books returns no match for this ISBN, so the configured fallback to Open Library
    // applies.
    seed(StubbedBibliographicGateways.ISBN_FALLBACK, null);

    mockMvc
        .perform(get("/api/books/" + StubbedBibliographicGateways.ISBN_FALLBACK).with(reader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Enriched by Open Library"));
  }

  @Test
  void aPresentLocalDescriptionIsKept() throws Exception {
    seed(StubbedBibliographicGateways.ISBN_LOCAL_KEPT, "Local description");

    mockMvc
        .perform(get("/api/books/" + StubbedBibliographicGateways.ISBN_LOCAL_KEPT).with(reader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Local description"));
  }

  @Test
  void aSourceFailureStillReturnsTheLocalBook() throws Exception {
    seed(StubbedBibliographicGateways.ISBN_FAIL, null);

    mockMvc
        .perform(get("/api/books/" + StubbedBibliographicGateways.ISBN_FAIL).with(reader()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isbn").value(StubbedBibliographicGateways.ISBN_FAIL))
        .andExpect(jsonPath("$.description").isEmpty());
  }

  private void seed(String isbn, String description) {
    bookRepository.save(new Book(isbn, "Local Title", description, genre, List.of(author), null));
  }

  private RequestPostProcessor reader() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_READER"));
  }
}
