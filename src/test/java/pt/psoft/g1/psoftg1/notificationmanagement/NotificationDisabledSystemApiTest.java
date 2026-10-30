package pt.psoft.g1.psoftg1.notificationmanagement;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest;
import pt.psoft.g1.psoftg1.notificationmanagement.infrastructure.NotificationRecord;
import pt.psoft.g1.psoftg1.notificationmanagement.infrastructure.NotificationRecordRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

/**
 * System test of the global disable switch (issue #22, ADR-0016, criterion C3.5). With {@code
 * notifications.enabled=false}, creating a lending through REST performs no delivery and writes no
 * audit record, the counterpart to the enabled fan-out test. Not transactional at the class level so
 * the create commits and the after-commit listener would run if the feature were on.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"notifications.enabled=false"})
class NotificationDisabledSystemApiTest {

  private static final String ISBN = "9782826012092";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AuthorRepository authorRepository;
  @Autowired private GenreRepository genreRepository;
  @Autowired private BookRepository bookRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ReaderRepository readerRepository;
  @Autowired private LendingRepository lendingRepository;
  @Autowired private NotificationRecordRepository auditRepository;

  private Author author;
  private Genre genre;
  private Book book;
  private Reader reader;
  private ReaderDetails readerDetails;
  private String lendingNumber;

  @BeforeEach
  void setUp() {
    author = authorRepository.save(new Author("Disabled Author", "A writer.", null));
    genre = genreRepository.save(new Genre("Disabled"));
    book =
        bookRepository.save(
            new Book(ISBN, "O Inspetor Max", "A police dog story.", genre, List.of(author), null));
    reader =
        userRepository.save(
            Reader.newReader("disabled.reader@example.com", "Manuelino123!", "Reader"));
    readerDetails =
        readerRepository.save(
            new ReaderDetails(991, reader, "2000-01-01", "919990991", true, true, true, null, null));
  }

  @AfterEach
  void tearDown() {
    if (lendingNumber != null) {
      auditRepository.deleteAll(auditRepository.findByLendingNumber(lendingNumber));
      lendingRepository.findByLendingNumber(lendingNumber).ifPresent(lendingRepository::delete);
    }
    readerRepository.delete(readerDetails);
    userRepository.delete(reader);
    bookRepository.delete(book);
    genreRepository.delete(genre);
    authorRepository.delete(author);
  }

  @Test
  void creatingALendingWithNotificationsDisabledWritesNoAuditRecord() throws Exception {
    final String body =
        objectMapper.writeValueAsString(
            new CreateLendingRequest(ISBN, readerDetails.getReaderNumber()));

    MvcResult created =
        mockMvc
            .perform(
                post("/api/lendings")
                    .with(librarian())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn();

    lendingNumber =
        objectMapper.readTree(created.getResponse().getContentAsString()).get("lendingNumber").asText();

    await()
        .during(1, SECONDS)
        .atMost(2, SECONDS)
        .untilAsserted(
            () -> {
              List<NotificationRecord> records = auditRepository.findByLendingNumber(lendingNumber);
              assertThat(records).isEmpty();
            });
  }

  private RequestPostProcessor librarian() {
    return jwt()
        .jwt(j -> j.claim("sub", "1,librarian@example.com"))
        .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
  }
}
