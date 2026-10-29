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
 * System-level (C3.5) acceptance tests of bibliographic enrichment through the external REST
 * interface (#6, ADR-0013), with the external gateways stubbed by {@link StubbedBibliographicGateways}.
 * A book is seeded locally and retrieved by ISBN, and the response is asserted to be enriched from the
 * configured source(s), to fall back across sources, and to return the local book when no source has
 * it. The enrichment-disabled case is in {@link BookEnrichmentDisabledApiTest}.
 */
@Transactional
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Import(StubbedBibliographicGateways.class)
class BookEnrichmentSystemApiTest {

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
  void retrievingABookWithAGapReturnsItEnrichedThroughRest() throws Exception {
    seed(StubbedBibliographicGateways.ISBN_ENRICHED, null);

    getBook(StubbedBibliographicGateways.ISBN_ENRICHED)
        .andExpect(jsonPath("$.isbn").value(StubbedBibliographicGateways.ISBN_ENRICHED))
        .andExpect(jsonPath("$.description").value("Enriched by Google Books"));
  }

  @Test
  void theConfiguredMultiSourceFallbackSurfacesThroughRest() throws Exception {
    // Google Books has no entry, so the second configured source, Open Library, supplies the data.
    seed(StubbedBibliographicGateways.ISBN_FALLBACK, null);

    getBook(StubbedBibliographicGateways.ISBN_FALLBACK)
        .andExpect(jsonPath("$.description").value("Enriched by Open Library"));
  }

  @Test
  void aBookNoSourceHasIsReturnedUnchanged() throws Exception {
    seed(StubbedBibliographicGateways.ISBN_NO_MATCH, null);

    getBook(StubbedBibliographicGateways.ISBN_NO_MATCH)
        .andExpect(jsonPath("$.isbn").value(StubbedBibliographicGateways.ISBN_NO_MATCH))
        .andExpect(jsonPath("$.description").isEmpty());
  }

  private org.springframework.test.web.servlet.ResultActions getBook(String isbn) throws Exception {
    return mockMvc.perform(get("/api/books/" + isbn).with(reader())).andExpect(status().isOk());
  }

  private void seed(String isbn, String description) {
    bookRepository.save(new Book(isbn, "Local Title", description, genre, List.of(author), null));
  }

  private RequestPostProcessor reader() {
    return jwt().authorities(new SimpleGrantedAuthority("ROLE_READER"));
  }
}
