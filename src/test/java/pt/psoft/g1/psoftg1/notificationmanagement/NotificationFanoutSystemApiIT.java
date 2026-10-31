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
import pt.psoft.g1.psoftg1.lendingmanagement.model.events.LendingEventType;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest;
import pt.psoft.g1.psoftg1.notificationmanagement.infrastructure.NotificationRecord;
import pt.psoft.g1.psoftg1.notificationmanagement.infrastructure.NotificationRecordRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

/**
 * System/component test of the notification fan-out and delivery audit through the real REST
 * interface (issue #22, ADR-0016, criteria C3.4 and C3.5). With email and SMS active in
 * configuration, a lending is created through {@code POST /api/lendings}; the SUT is the
 * controller, the service, the after-commit listener, the composite notifier, the two channels with
 * their self-contained gateways, and the audit repository. It is not transactional at the class
 * level, so the create really commits and the after-commit notification runs. The test asserts the
 * persisted outcome: one SENT audit record per active mechanism for the created lending, which is
 * the stable end-to-end evidence ADR-0016 chose the audit to provide.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"notifications.enabled=true", "notifications.channels=email,sms"})
class NotificationFanoutSystemApiIT {

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
    author = authorRepository.save(new Author("Fanout Author", "A writer.", null));
    genre = genreRepository.save(new Genre("Fanout"));
    book =
        bookRepository.save(
            new Book(ISBN, "O Inspetor Max", "A police dog story.", genre, List.of(author), null));
    reader =
        userRepository.save(
            Reader.newReader("fanout.reader@example.com", "Manuelino123!", "Reader"));
    readerDetails =
        readerRepository.save(
            new ReaderDetails(
                990, reader, "2000-01-01", "919990990", true, true, true, null, null));
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
  void creatingALendingFansOutToTheActiveMechanismsAndAuditsEachAsSent() throws Exception {
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
        objectMapper
            .readTree(created.getResponse().getContentAsString())
            .get("lendingNumber")
            .asText();

    await()
        .atMost(5, SECONDS)
        .untilAsserted(
            () -> {
              List<NotificationRecord> records = auditRepository.findByLendingNumber(lendingNumber);
              assertThat(records)
                  .extracting(NotificationRecord::getMechanism)
                  .containsExactlyInAnyOrder("email", "sms");
              assertThat(records)
                  .allSatisfy(
                      r -> {
                        assertThat(r.getOutcome()).isEqualTo(NotificationRecord.Outcome.SENT);
                        assertThat(r.getEventType()).isEqualTo(LendingEventType.CREATED);
                        assertThat(r.getReaderNumber()).isEqualTo(readerDetails.getReaderNumber());
                      });
            });
  }

  private RequestPostProcessor librarian() {
    return jwt()
        .jwt(j -> j.claim("sub", "1,librarian@example.com"))
        .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
  }
}
