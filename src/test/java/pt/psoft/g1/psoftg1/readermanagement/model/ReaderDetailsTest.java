package pt.psoft.g1.psoftg1.readermanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.services.UpdateReaderRequest;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

class ReaderDetailsTest {
  private static final long PERSISTED_VERSION = 2L;

  private Reader reader;
  private ReaderDetails readerDetails;

  private ReaderDetails newDetails(
      String birthDate, String phone, boolean gdpr, String photo, List<Genre> interests) {
    return new ReaderDetails(5, reader, birthDate, phone, gdpr, false, false, photo, interests);
  }

  private void patch(
      long version, UpdateReaderRequest request, String photo, List<Genre> interests) {
    readerDetails.applyPatch(
        version,
        request.getUsername(),
        request.getPassword(),
        request.getFullName(),
        request.getBirthDate(),
        request.getPhoneNumber(),
        request.getMarketing(),
        request.getThirdParty(),
        photo,
        interests);
  }

  @BeforeEach
  void setUp() {
    reader = Reader.newReader("manuel@gmail.com", "Manuelino123!", "Manuel Coives");
    readerDetails = newDetails("2000-01-01", "919191919", true, "reader.png", null);
    ReflectionTestUtils.setField(readerDetails, "version", PERSISTED_VERSION);
  }

  @Test
  void ensureConstructorSetsAllFields() {
    ReaderDetails details =
        new ReaderDetails(
            5, reader, "2000-01-01", "919191919", true, true, false, "reader.png", null);

    assertSame(reader, details.getReader());
    assertEquals(LocalDate.now().getYear() + "/5", details.getReaderNumber());
    assertEquals("919191919", details.getPhoneNumber());
    assertEquals(LocalDate.of(2000, 1, 1), details.getBirthDate().getBirthDate());
    assertTrue(details.isGdprConsent());
    assertTrue(details.isMarketingConsent());
    assertFalse(details.isThirdPartySharingConsent());
    assertEquals("reader.png", details.getPhoto().getPhotoFile());
  }

  @Test
  void ensureReaderIsRequired() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ReaderDetails(5, null, "2000-01-01", "919191919", true, false, false, null, null));
  }

  @Test
  void ensurePhoneNumberIsRequired() {
    assertThrows(
        IllegalArgumentException.class, () -> newDetails("2000-01-01", null, true, null, null));
  }

  @Test
  void ensureReaderMustConsentToGdpr() {
    assertThrows(
        IllegalArgumentException.class,
        () -> newDetails("2000-01-01", "919191919", false, null, null));
  }

  @Test
  void ensureInvalidPhoneNumberIsRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> newDetails("2000-01-01", "123", true, null, null));
  }

  @Test
  void ensureInvalidBirthDateIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> newDetails("01-01-2000", "919191919", true, null, null));
  }

  @Test
  void ensureBirthDateCannotBeInTheFuture() {
    String tomorrow = LocalDate.now().plusDays(1).toString();
    assertThrows(
        AccessDeniedException.class, () -> newDetails(tomorrow, "919191919", true, null, null));
  }

  @Test
  void ensurePhotoIsOptional() {
    assertNull(newDetails("2000-01-01", "919191919", true, null, null).getPhoto());
  }

  @Test
  void ensureInterestListIsOptional() {
    assertNull(newDetails("2000-01-01", "919191919", true, null, null).getInterestList());
    assertTrue(
        newDetails("2000-01-01", "919191919", true, null, List.of()).getInterestList().isEmpty());
  }

  @Test
  void ensureInterestListKeepsGivenGenres() {
    List<Genre> genres = List.of(new Genre("Fantasia"), new Genre("Terror"));

    assertEquals(
        genres, newDetails("2000-01-01", "919191919", true, null, genres).getInterestList());
  }

  @Test
  void ensurePatchWithStaleVersionFails() {
    UpdateReaderRequest request = new UpdateReaderRequest();
    request.setPhoneNumber("929292929");

    assertThrows(ConflictException.class, () -> patch(1L, request, null, null));
    assertEquals("919191919", readerDetails.getPhoneNumber());
  }

  @Test
  void ensurePatchUpdatesReaderAndDetails() {
    Genre genre = new Genre("Fantasia");
    UpdateReaderRequest request = new UpdateReaderRequest();
    request.setUsername("novo@gmail.com");
    request.setFullName("Novo Nome");
    request.setPassword("NovaPass123!");
    request.setBirthDate("1999-12-31");
    request.setPhoneNumber("929292929");
    request.setMarketing(true);
    request.setThirdParty(true);

    patch(PERSISTED_VERSION, request, "new.png", List.of(genre));

    assertEquals("novo@gmail.com", reader.getUsername());
    assertEquals("Novo Nome", reader.getName().toString());
    assertTrue(new BCryptPasswordEncoder().matches("NovaPass123!", reader.getPassword()));
    assertEquals(LocalDate.of(1999, 12, 31), readerDetails.getBirthDate().getBirthDate());
    assertEquals("929292929", readerDetails.getPhoneNumber());
    assertTrue(readerDetails.isMarketingConsent());
    assertTrue(readerDetails.isThirdPartySharingConsent());
    assertEquals("new.png", readerDetails.getPhoto().getPhotoFile());
    assertEquals(List.of(genre), readerDetails.getInterestList());
  }

  @Test
  void ensurePatchKeepsValuesThatAreNotProvided() {
    String encodedPassword = reader.getPassword();

    patch(PERSISTED_VERSION, new UpdateReaderRequest(), null, null);

    assertEquals("manuel@gmail.com", reader.getUsername());
    assertEquals("Manuel Coives", reader.getName().toString());
    assertEquals(encodedPassword, reader.getPassword());
    assertEquals(LocalDate.of(2000, 1, 1), readerDetails.getBirthDate().getBirthDate());
    assertEquals("919191919", readerDetails.getPhoneNumber());
    assertEquals("reader.png", readerDetails.getPhoto().getPhotoFile());
    assertNull(readerDetails.getInterestList());
  }

  @Test
  void ensurePatchWithInvalidPhoneNumberFails() {
    UpdateReaderRequest request = new UpdateReaderRequest();
    request.setPhoneNumber("123");

    assertThrows(
        IllegalArgumentException.class, () -> patch(PERSISTED_VERSION, request, null, null));
    assertEquals("919191919", readerDetails.getPhoneNumber());
  }

  @Test
  void ensurePatchWithWeakPasswordFails() {
    String encodedPassword = reader.getPassword();
    UpdateReaderRequest request = new UpdateReaderRequest();
    request.setPassword("weak");

    assertThrows(
        IllegalArgumentException.class, () -> patch(PERSISTED_VERSION, request, null, null));
    assertEquals(encodedPassword, reader.getPassword());
  }

  @Test
  void ensurePatchWithInvalidNameFails() {
    UpdateReaderRequest request = new UpdateReaderRequest();
    request.setFullName("M@nuel");

    assertThrows(
        IllegalArgumentException.class, () -> patch(PERSISTED_VERSION, request, null, null));
    assertEquals("Manuel Coives", reader.getName().toString());
  }

  @Test
  void ensurePatchWithInvalidBirthDateFails() {
    UpdateReaderRequest request = new UpdateReaderRequest();
    request.setBirthDate("31/12/1999");

    assertThrows(
        IllegalArgumentException.class, () -> patch(PERSISTED_VERSION, request, null, null));
    assertEquals(LocalDate.of(2000, 1, 1), readerDetails.getBirthDate().getBirthDate());
  }

  @Test
  void ensureRemovePhotoWithStaleVersionFails() {
    assertThrows(ConflictException.class, () -> readerDetails.removePhoto(1L));
    assertNotNull(readerDetails.getPhoto());
  }

  @Test
  void ensureRemovePhotoClearsPhoto() {
    readerDetails.removePhoto(PERSISTED_VERSION);
    assertNull(readerDetails.getPhoto());
  }
}
