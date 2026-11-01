package pt.psoft.g1.psoftg1.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.services.CreateLendingRequest;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationAndAuthorisationSystemApiIT {

  private static final String PASSWORD = "Manuelino123!";
  private static final String LIBRARIAN = "sec.librarian@example.com";
  private static final String READER = "sec.reader@example.com";
  private static final String OTHER_READER = "sec.other@example.com";
  private static final String ISBN = "9782826012092";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private ReaderRepository readerRepository;
  @Autowired private AuthorRepository authorRepository;
  @Autowired private GenreRepository genreRepository;
  @Autowired private BookRepository bookRepository;

  private ReaderDetails reader;

  @BeforeEach
  void setUp() {
    userRepository.save(Librarian.newLibrarian(LIBRARIAN, PASSWORD, "Librarian"));
    reader =
        readerRepository.save(
            new ReaderDetails(
                981,
                userRepository.save(Reader.newReader(READER, PASSWORD, "Reader")),
                "2000-01-01",
                "919990981",
                true,
                true,
                true,
                null,
                null));
    readerRepository.save(
        new ReaderDetails(
            982,
            userRepository.save(Reader.newReader(OTHER_READER, PASSWORD, "Other")),
            "2000-01-01",
            "919990982",
            true,
            true,
            true,
            null,
            null));
    Author author = authorRepository.save(new Author("Security Author", "A writer.", null));
    Genre genre = genreRepository.save(new Genre("Security"));
    bookRepository.save(new Book(ISBN, "O Inspetor Max", "A story.", genre, List.of(author), null));
  }

  @Test
  void loginWithValidCredentialsReturnsATokenAndTheUser() throws Exception {
    mockMvc
        .perform(login(READER, PASSWORD))
        .andExpect(status().isOk())
        .andExpect(header().exists(HttpHeaders.AUTHORIZATION))
        .andExpect(jsonPath("$.username").value(READER));
  }

  @Test
  void loginWithAWrongPasswordIsRejected() throws Exception {
    mockMvc.perform(login(READER, "Wrong123!pass")).andExpect(status().isUnauthorized());
  }

  @Test
  void aProtectedEndpointRejectsARequestWithoutAToken() throws Exception {
    mockMvc.perform(get("/api/authors").param("name", "x")).andExpect(status().isUnauthorized());
  }

  @Test
  void aTokenFromLoginGrantsTheOperationsOfItsRole() throws Exception {
    mockMvc
        .perform(
            get("/api/authors")
                .param("name", "Security")
                .header(HttpHeaders.AUTHORIZATION, tokenOf(READER)))
        .andExpect(status().isOk());
  }

  @Test
  void aReaderCannotUseALibrarianOperation() throws Exception {
    mockMvc
        .perform(
            post("/api/authors")
                .header(HttpHeaders.AUTHORIZATION, tokenOf(READER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void aLibrarianCannotUseAReaderOnlyOperation() throws Exception {
    Long author = authorRepository.searchByNameName("Security Author").get(0).getAuthorNumber();

    mockMvc
        .perform(
            get("/api/authors/" + author + "/books")
                .header(HttpHeaders.AUTHORIZATION, tokenOf(LIBRARIAN)))
        .andExpect(status().isForbidden());
  }

  @Test
  void aReaderSeesTheirOwnLendingButNotAnotherReaders() throws Exception {
    String lending = lendingOf(reader);

    mockMvc
        .perform(get("/api/lendings/" + lending).header(HttpHeaders.AUTHORIZATION, tokenOf(READER)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/api/lendings/" + lending)
                .header(HttpHeaders.AUTHORIZATION, tokenOf(OTHER_READER)))
        .andExpect(status().isForbidden());
  }

  @Test
  void aLibrarianSeesAnyReadersLending() throws Exception {
    String lending = lendingOf(reader);

    mockMvc
        .perform(
            get("/api/lendings/" + lending).header(HttpHeaders.AUTHORIZATION, tokenOf(LIBRARIAN)))
        .andExpect(status().isOk());
  }

  private String lendingOf(ReaderDetails details) throws Exception {
    String body =
        objectMapper.writeValueAsString(new CreateLendingRequest(ISBN, details.getReaderNumber()));
    String response =
        mockMvc
            .perform(
                post("/api/lendings")
                    .header(HttpHeaders.AUTHORIZATION, tokenOf(LIBRARIAN))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(response).get("lendingNumber").asText();
  }

  private String tokenOf(String username) throws Exception {
    String token =
        mockMvc
            .perform(login(username, PASSWORD))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader(HttpHeaders.AUTHORIZATION);
    assertThat(token).isNotBlank();
    return "Bearer " + token;
  }

  private RequestBuilder login(String username, String password) throws Exception {
    return post("/api/public/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            objectMapper.writeValueAsString(Map.of("username", username, "password", password)));
  }
}
