package pt.psoft.g1.psoftg1.lendingmanagement.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

/**
 * System-level (C3.5) acceptance tests of the lending chain through the external REST interfaces
 * (issue #13, ADR-0007). A librarian creates a lending and the owning reader renews it, both over
 * MockMvc with controlled reader histories, so eligibility, the lending period, the renewed limit
 * date, the {@code MaxRenewals} limit, and the re-evaluation of the applicable policy on a new
 * lending and on a renewal are all verified from the outside.
 *
 * <p>Re-evaluation is demonstrated by contrasting two readers against the same configuration: a
 * reader with no qualifying history is governed by the Standard policy (a 15-day period), whereas a
 * reader whose recent history qualifies is governed by the Adaptive policy (a 21-day period for the
 * lowest band). The period that the renewal actually applies is the evidence that the policy is
 * chosen at the moment of the request, not fixed when the lending was created.
 */
@Transactional
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class LendingRenewalSystemApiIT {

  private static final String ISBN = "9782826012092";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private LendingRepository lendingRepository;
  @Autowired private ReaderRepository readerRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private BookRepository bookRepository;
  @Autowired private GenreRepository genreRepository;
  @Autowired private AuthorRepository authorRepository;

  private Book book;
  private int historySeq = 700;

  @BeforeEach
  void setUp() {
    Author author =
        new Author("Manuel Antonio Pina", "Escritor portugues premiado com o Premio Camoes", null);
    authorRepository.save(author);
    Genre genre = new Genre("Genero");
    genreRepository.save(genre);
    book =
        new Book(
            ISBN,
            "O Inspetor Max",
            "Um pastor-alemao que trabalha para a Judiciaria ajuda a resolver um caso",
            genre,
            List.of(author),
            null);
    bookRepository.save(book);
  }

  @Test
  void standardReaderCanLendThenRenewThroughRest() throws Exception {
    final ReaderDetails reader = seedReader(1, "standard.reader@example.com");

    // A reader with no history is governed by the Standard policy: a 15-day lending period.
    final MvcResult created = createLending(reader);
    final LocalDate createdLimit = LocalDate.now().plusDays(15);
    assertLimitDate(created, createdLimit);

    // The renewal re-selects the Standard policy and extends by its 15-day renewal period.
    renew(reader, created)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.renewalCount").value(1))
        .andExpect(jsonPath("$.limitDate").value(createdLimit.plusDays(15).toString()));
  }

  @Test
  void qualifyingReaderRenewsUnderTheAdaptivePolicy() throws Exception {
    final ReaderDetails reader = seedReader(2, "adaptive.reader@example.com");
    seedQualifyingHistory(reader); // 5 completed, all on time, within the last 12 months

    // Re-evaluated at creation to the Adaptive policy: with no active lendings the lowest band
    // applies, a 21-day period, which is distinct from the Standard 15.
    final MvcResult created = createLending(reader);
    final LocalDate createdLimit = LocalDate.now().plusDays(21);
    assertLimitDate(created, createdLimit);

    // Re-evaluated again at renewal: still Adaptive, so the renewed period is the 21-day band
    // period rather than the Standard 15, which proves the policy is chosen per request.
    renew(reader, created)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.renewalCount").value(1))
        .andExpect(jsonPath("$.limitDate").value(createdLimit.plusDays(21).toString()));
  }

  @Test
  void qualifyingReaderHasNoRenewalCapUnderTheAdaptivePolicy() throws Exception {
    final ReaderDetails reader = seedReader(4, "adaptive.nocap@example.com");
    seedQualifyingHistory(reader);
    final MvcResult created = createLending(reader);
    final String lendingNumber = lendingNumberOf(created);

    // The Standard policy caps renewals at 3; the Adaptive policy has no cap, so a qualifying
    // reader may renew beyond that limit. Four successive renewals all succeed.
    for (int i = 1; i <= 4; i++) {
      renewCurrent(reader, lendingNumber)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.renewalCount").value(i));
    }
  }

  @Test
  void standardReaderRenewalIsRefusedAtTheMaxRenewalsLimit() throws Exception {
    final ReaderDetails reader = seedReader(3, "boundary.reader@example.com");
    final MvcResult created = createLending(reader);

    final String lendingNumber = lendingNumberOf(created);
    // Three renewals are allowed under the Standard policy (maxRenewals = 3).
    for (int i = 1; i <= 3; i++) {
      renewCurrent(reader, lendingNumber)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.renewalCount").value(i));
    }
    // The fourth renewal crosses the MaxRenewals boundary and is refused.
    renewCurrent(reader, lendingNumber).andExpect(status().isForbidden());
  }

  // --- REST interactions ---

  private MvcResult createLending(ReaderDetails reader) throws Exception {
    final String body =
        objectMapper.writeValueAsString(
            new pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest(
                ISBN, reader.getReaderNumber()));
    return mockMvc
        .perform(
            post("/api/lendings")
                .with(librarian())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isCreated())
        .andReturn();
  }

  /** Renews the lending created by {@code created}, using the ETag it returned as the version. */
  private org.springframework.test.web.servlet.ResultActions renew(
      ReaderDetails reader, MvcResult created) throws Exception {
    final String ln = lendingNumberOf(created);
    final String etag = created.getResponse().getHeader(HttpHeaders.ETAG);
    return mockMvc.perform(
        patch("/api/lendings/" + ln + "/renew")
            .with(readerPrincipal(reader))
            .header(HttpHeaders.IF_MATCH, etag));
  }

  /** Renews the current state of the lending, reading the live version from the repository. */
  private org.springframework.test.web.servlet.ResultActions renewCurrent(
      ReaderDetails reader, String lendingNumber) throws Exception {
    final long version =
        lendingRepository.findByLendingNumber(lendingNumber).orElseThrow().getVersion();
    return mockMvc.perform(
        patch("/api/lendings/" + lendingNumber + "/renew")
            .with(readerPrincipal(reader))
            .header(HttpHeaders.IF_MATCH, Long.toString(version)));
  }

  private String lendingNumberOf(MvcResult result) throws Exception {
    return objectMapper
        .readTree(result.getResponse().getContentAsString())
        .get("lendingNumber")
        .asText();
  }

  private void assertLimitDate(MvcResult result, LocalDate expected) throws Exception {
    final String actual =
        objectMapper.readTree(result.getResponse().getContentAsString()).get("limitDate").asText();
    org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(expected.toString());
  }

  // --- fixtures ---

  private ReaderDetails seedReader(int number, String username) {
    final Reader user = Reader.newReader(username, "Manuelino123!", "Reader " + number);
    userRepository.save(user);
    final ReaderDetails details =
        new ReaderDetails(
            number, user, "2000-01-01", "91919191" + number, true, true, true, null, null);
    return readerRepository.save(details);
  }

  /** Five completed, on-time lendings returned within the last twelve months. */
  private void seedQualifyingHistory(ReaderDetails reader) {
    for (int i = 0; i < 5; i++) {
      final LocalDate start = LocalDate.now().minusMonths(2).minusDays(i);
      lendingRepository.save(
          Lending.newBootstrappingLending(
              book,
              reader,
              LocalDate.now().getYear(),
              historySeq++,
              start,
              start.plusDays(5),
              15,
              300));
    }
  }

  private RequestPostProcessor librarian() {
    return jwt()
        .jwt(j -> j.claim("sub", "1,librarian@example.com"))
        .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
  }

  private RequestPostProcessor readerPrincipal(ReaderDetails reader) {
    return jwt()
        .jwt(j -> j.claim("sub", "1," + reader.getReader().getUsername()))
        .authorities(new SimpleGrantedAuthority("ROLE_READER"));
  }
}
