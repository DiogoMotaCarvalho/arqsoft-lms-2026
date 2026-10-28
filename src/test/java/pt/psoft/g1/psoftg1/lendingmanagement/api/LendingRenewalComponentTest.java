package pt.psoft.g1.psoftg1.lendingmanagement.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
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
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

/**
 * Component-level (C3.4) tests of the renewal flow with the SUT equal to the controller, the
 * service, the domain policy, and the repository, driven through the real REST endpoint over H2
 * (issue #13, ADR-0007). The lending flow has no external gateways to substitute: the bibliographic
 * gateways (Google Books, Open Library) belong to the Book flow, so there is nothing to fake here,
 * and the whole stack is real. The caller is a stub JWT principal with no real credentials, resolved
 * by the controller against a seeded reader fixture.
 *
 * <p>Each case asserts the externally observable outcome (the HTTP status, the renewed limit date,
 * and the renewal count) rather than any internal structure. The fixture reader has no completed
 * history, so the selected policy is the Standard policy (renewal period 15 days, cap of 3
 * renewals).
 */
@Transactional
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
class LendingRenewalComponentTest {

  private static final String ISBN = "9782826012092";

  @Autowired private MockMvc mockMvc;
  @Autowired private LendingRepository lendingRepository;
  @Autowired private ReaderRepository readerRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private BookRepository bookRepository;
  @Autowired private GenreRepository genreRepository;
  @Autowired private AuthorRepository authorRepository;

  private Author author;
  private Genre genre;
  private Book book;
  private Reader reader;
  private ReaderDetails readerDetails;
  private int seqCounter = 500;

  @BeforeEach
  void setUp() {
    author = new Author("Manuel Antonio Pina", "Escritor portugues premiado com o Premio Camoes", null);
    authorRepository.save(author);

    genre = new Genre("Genero");
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

    reader = Reader.newReader("renew.reader@example.com", "Manuelino123!", "Renew Reader");
    userRepository.save(reader);

    readerDetails =
        new ReaderDetails(1, reader, "2000-01-01", "919191919", true, true, true, null, null);
    readerRepository.save(readerDetails);
  }

  @Test
  void standardRenewalExtendsLimitByFifteenDaysAndIncrementsCount() throws Exception {
    final Lending lending = activeLending(LocalDate.now(), 15);
    final LocalDate expected = lending.getLimitDate().plusDays(15);

    mockMvc
        .perform(renew(lending).with(owner()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.renewalCount").value(1))
        .andExpect(jsonPath("$.limitDate").value(expected.toString()));

    final Lending reloaded =
        lendingRepository.findByLendingNumber(lending.getLendingNumber()).orElseThrow();
    assertThat(reloaded.getRenewalCount()).isEqualTo(1);
    assertThat(reloaded.getLimitDate()).isEqualTo(expected);
  }

  @Test
  void renewalDeniedWhenOverdue() throws Exception {
    // limit date in the past, not yet returned -> overdue
    final Lending lending = activeLending(LocalDate.now().minusDays(30), 15);

    mockMvc.perform(renew(lending).with(owner())).andExpect(status().isForbidden());

    assertThat(
            lendingRepository.findByLendingNumber(lending.getLendingNumber()).orElseThrow().getRenewalCount())
        .isZero();
  }

  @Test
  void renewalDeniedAtMaxRenewalsBoundary() throws Exception {
    Lending lending = activeLending(LocalDate.now(), 15);

    // three renewals are allowed under the Standard policy (maxRenewals = 3)
    for (int i = 1; i <= 3; i++) {
      long version =
          lendingRepository.findByLendingNumber(lending.getLendingNumber()).orElseThrow().getVersion();
      mockMvc
          .perform(renew(lending, version).with(owner()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.renewalCount").value(i));
    }

    // the fourth renewal crosses the MaxRenewals boundary and is refused
    long version =
        lendingRepository.findByLendingNumber(lending.getLendingNumber()).orElseThrow().getVersion();
    mockMvc.perform(renew(lending, version).with(owner())).andExpect(status().isForbidden());
  }

  @Test
  void renewalRejectedForAnAlreadyReturnedLending() throws Exception {
    // returned on time, so the policy would allow it, but the entity refuses to renew a returned one
    final Lending lending =
        save(
            Lending.newBootstrappingLending(
                book, readerDetails, LocalDate.now().getYear(), nextSeq(),
                LocalDate.now().minusDays(5), LocalDate.now().minusDays(1), 15, 300));

    mockMvc.perform(renew(lending).with(owner())).andExpect(status().isBadRequest());
  }

  @Test
  void renewalRejectedForAStaleVersion() throws Exception {
    final Lending lending = activeLending(LocalDate.now(), 15);

    mockMvc
        .perform(renew(lending, lending.getVersion() - 1).with(owner()))
        .andExpect(status().isConflict());
  }

  @Test
  void renewalForbiddenForAReaderThatDoesNotOwnTheLending() throws Exception {
    final Lending lending = activeLending(LocalDate.now(), 15);

    final Reader otherUser =
        Reader.newReader("other.reader@example.com", "Manuelino123!", "Other Reader");
    userRepository.save(otherUser);
    final ReaderDetails other =
        new ReaderDetails(2, otherUser, "2000-01-01", "929292929", true, true, true, null, null);
    readerRepository.save(other);

    mockMvc
        .perform(renew(lending).with(principal("other.reader@example.com")))
        .andExpect(status().isForbidden());
  }

  @Test
  void renewalWithoutIfMatchIsABadRequest() throws Exception {
    final Lending lending = activeLending(LocalDate.now(), 15);

    mockMvc
        .perform(patch(renewUri(lending)).with(owner()))
        .andExpect(status().isBadRequest());
  }

  // --- fixture helpers ---

  private Lending activeLending(LocalDate startDate, int durationInDays) {
    return save(
        Lending.newBootstrappingLending(
            book, readerDetails, LocalDate.now().getYear(), nextSeq(), startDate, null,
            durationInDays, 300));
  }

  private Lending save(Lending lending) {
    return lendingRepository.save(lending);
  }

  private int nextSeq() {
    return seqCounter++;
  }

  private String renewUri(Lending lending) {
    return "/api/lendings/" + lending.getLendingNumber() + "/renew";
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder renew(
      Lending lending) {
    return renew(lending, lending.getVersion());
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder renew(
      Lending lending, long version) {
    return patch(renewUri(lending)).header(HttpHeaders.IF_MATCH, Long.toString(version));
  }

  private RequestPostProcessor owner() {
    return principal("renew.reader@example.com");
  }

  /** A stub JWT principal for the given reader username, with the READER role. */
  private RequestPostProcessor principal(String username) {
    return jwt()
        .jwt(j -> j.claim("sub", "1," + username))
        .authorities(new SimpleGrantedAuthority("ROLE_READER"));
  }
}
