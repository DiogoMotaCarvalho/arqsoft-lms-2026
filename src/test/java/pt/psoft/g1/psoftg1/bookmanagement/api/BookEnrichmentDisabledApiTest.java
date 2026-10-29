package pt.psoft.g1.psoftg1.bookmanagement.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

/**
 * System-level test that with {@code bibliographic.enrichment.enabled=false} a retrieval is purely
 * local and no gateway is consulted (#6, ADR-0013). If the gateway were consulted, the stub would
 * enrich the blank description; asserting it stays blank proves the toggle suppresses the external
 * call. This is the enrichment-off half of the C3.5 evidence.
 */
@Transactional
@SpringBootTest(properties = "bibliographic.enrichment.enabled=false")
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@Import(StubbedBibliographicGateways.class)
class BookEnrichmentDisabledApiTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthorRepository authorRepository;
  @Autowired private GenreRepository genreRepository;
  @Autowired private BookRepository bookRepository;

  @Test
  void withEnrichmentOffARetrievalIsPurelyLocal() throws Exception {
    Author author = authorRepository.save(new Author("Ana", "A Portuguese author.", null));
    Genre genre = genreRepository.save(new Genre("Genero"));
    // seeded with a blank description that the stub would otherwise fill
    bookRepository.save(
        new Book(
            StubbedBibliographicGateways.ISBN_ENRICHED,
            "Local Title",
            null,
            genre,
            List.of(author),
            null));

    mockMvc
        .perform(
            get("/api/books/" + StubbedBibliographicGateways.ISBN_ENRICHED)
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_READER"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").isEmpty());
  }
}
