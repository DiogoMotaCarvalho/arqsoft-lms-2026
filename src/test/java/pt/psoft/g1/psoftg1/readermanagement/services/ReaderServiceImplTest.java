package pt.psoft.g1.psoftg1.readermanagement.services;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.InMemoryGenreRepository;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.InMemoryReaderRepository;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.InMemoryForbiddenNameRepository;
import pt.psoft.g1.psoftg1.shared.repositories.InMemoryPhotoRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.repositories.InMemoryUserRepository;

class ReaderServiceImplTest {
  private static final String CURRENT_YEAR = String.valueOf(LocalDate.now().getYear());

  private final InMemoryReaderRepository readerRepository = new InMemoryReaderRepository();
  private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
  private final InMemoryGenreRepository genreRepository = new InMemoryGenreRepository();
  private final InMemoryForbiddenNameRepository forbiddenNameRepository =
      new InMemoryForbiddenNameRepository();
  private final InMemoryPhotoRepository photoRepository = new InMemoryPhotoRepository();

  private final ReaderServiceImpl service =
      new ReaderServiceImpl(
          readerRepository,
          userRepository,
          new ReaderMapperImpl(),
          genreRepository,
          forbiddenNameRepository,
          photoRepository);

  private static CreateReaderRequest registration(String username, String fullName) {
    CreateReaderRequest request = new CreateReaderRequest();
    request.setUsername(username);
    request.setPassword("Manuelino123!");
    request.setFullName(fullName);
    request.setBirthDate("2000-01-01");
    request.setPhoneNumber("919191919");
    request.setGdpr(true);
    return request;
  }

  private ReaderDetails existingReader(String photoUri) {
    Reader reader =
        userRepository.save(Reader.newReader("maria@gmail.com", "Mariazinha123!", "Maria Silva"));
    return readerRepository.save(
        new ReaderDetails(
            1, reader, "1990-05-10", "929292929", true, false, false, photoUri, null));
  }

  @Test
  void ensureReaderIsRegisteredWithUserAccountAndDetails() {
    ReaderDetails created = service.create(registration("manuel@gmail.com", "Manuel Coives"), null);

    Reader reader = created.getReader();
    assertEquals("manuel@gmail.com", reader.getUsername());
    assertTrue(reader.getAuthorities().contains(new Role(Role.READER)));
    assertEquals(List.of(reader), userRepository.findAll());
    assertEquals(List.of(created), readerRepository.findAll());
  }

  @Test
  void ensureReaderNumbersFollowTheYearlySequence() {
    existingReader(null);

    ReaderDetails created = service.create(registration("manuel@gmail.com", "Manuel Coives"), null);

    assertEquals(CURRENT_YEAR + "/2", created.getReaderNumber());
  }

  @Test
  void ensureUsernameMustBeUnique() {
    ReaderDetails existing = existingReader(null);
    CreateReaderRequest request = registration("maria@gmail.com", "Outra Maria");

    assertThrows(ConflictException.class, () -> service.create(request, null));
    assertEquals(List.of(existing), readerRepository.findAll());
  }

  @Test
  void ensureNameCannotContainForbiddenWords() {
    forbiddenNameRepository.save(new ForbiddenName("Coiv"));
    CreateReaderRequest request = registration("manuel@gmail.com", "Manuel Coives");

    assertThrows(IllegalArgumentException.class, () -> service.create(request, null));
    assertTrue(userRepository.findAll().isEmpty());
  }

  @Test
  void ensureInterestsAreResolvedToGenres() {
    Genre fantasy = genreRepository.save(new Genre("Fantasia"));
    Genre horror = genreRepository.save(new Genre("Terror"));
    CreateReaderRequest request = registration("manuel@gmail.com", "Manuel Coives");
    request.setInterestList(List.of("Fantasia", "Terror"));

    ReaderDetails created = service.create(request, null);

    assertEquals(List.of(fantasy, horror), created.getInterestList());
  }

  @Test
  void ensureInterestsMustBeExistingGenres() {
    CreateReaderRequest request = registration("manuel@gmail.com", "Manuel Coives");
    request.setInterestList(List.of("Inexistente"));

    assertThrows(NotFoundException.class, () -> service.create(request, null));
    assertTrue(userRepository.findAll().isEmpty());
  }

  @Test
  void ensurePhotoIsStoredWhenFileAndUriAreProvided() {
    CreateReaderRequest request = registration("manuel@gmail.com", "Manuel Coives");
    request.setPhoto(new MockMultipartFile("photo", "me.png", "image/png", new byte[] {1}));

    ReaderDetails created = service.create(request, "me.png");

    assertEquals("me.png", created.getPhoto().getPhotoFile());
  }

  @Test
  void ensureUpdateAppliesChangesToReaderAndDetails() {
    ReaderDetails existing = existingReader(null);
    UpdateReaderRequest request = new UpdateReaderRequest();
    request.setFullName("Maria Santos");
    request.setPhoneNumber("912345678");

    ReaderDetails updated =
        service.update(existing.getReader().getId(), request, existing.getVersion(), null);

    assertEquals("Maria Santos", updated.getReader().getName().toString());
    assertEquals("912345678", updated.getPhoneNumber());
  }

  @Test
  void ensureCannotUpdateUnknownReader() {
    UpdateReaderRequest request = new UpdateReaderRequest();

    assertThrows(NotFoundException.class, () -> service.update(99L, request, 0, null));
  }

  @Test
  void ensureCannotUpdateWithStaleVersion() {
    ReaderDetails existing = existingReader(null);
    UpdateReaderRequest request = new UpdateReaderRequest();
    request.setPhoneNumber("912345678");
    Long userId = existing.getReader().getId();

    assertThrows(ConflictException.class, () -> service.update(userId, request, 5, null));
    assertEquals("929292929", existing.getPhoneNumber());
  }

  @Test
  void ensureRemovingPhotoAlsoDeletesTheFile() {
    ReaderDetails existing = existingReader("maria.png");

    service.removeReaderPhoto(existing.getReaderNumber(), existing.getVersion());

    assertNull(existing.getPhoto());
    assertEquals(List.of("maria.png"), photoRepository.deletedPhotoFiles());
  }

  @Test
  void ensurePhotoIsKeptWhenRemovingWithStaleVersion() {
    ReaderDetails existing = existingReader("maria.png");
    String readerNumber = existing.getReaderNumber();

    assertThrows(ConflictException.class, () -> service.removeReaderPhoto(readerNumber, 5));
    assertNotNull(existing.getPhoto());
    assertTrue(photoRepository.deletedPhotoFiles().isEmpty());
  }

  @Test
  void ensureTopByGenrePeriodCannotEndBeforeItStarts() {
    LocalDate start = LocalDate.of(2024, 6, 1);
    LocalDate end = LocalDate.of(2024, 5, 31);

    assertThrows(
        IllegalArgumentException.class, () -> service.findTopByGenre("Fantasia", start, end));
  }

  @Test
  void ensureTopReadersMustAskForAtLeastOne() {
    assertThrows(IllegalArgumentException.class, () -> service.findTopReaders(0));
  }
}
