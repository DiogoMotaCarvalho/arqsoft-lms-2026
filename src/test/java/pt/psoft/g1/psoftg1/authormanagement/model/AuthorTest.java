package pt.psoft.g1.psoftg1.authormanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.shared.model.Photo;

class AuthorTest {
  private final String validName = "João Alberto";
  private final String validBio =
      "O João Alberto nasceu em Chaves e foi pedreiro a maior parte da sua vida.";

  @Test
  void ensureNameNotNull() {
    assertThrows(IllegalArgumentException.class, () -> new Author(null, validBio, null));
  }

  @Test
  void ensureNameMustBeAlphanumeric() {
    assertThrows(IllegalArgumentException.class, () -> new Author("João@Alberto", validBio, null));
  }

  @Test
  void ensureBioNotNull() {
    assertThrows(IllegalArgumentException.class, () -> new Author(validName, null, null));
  }

  @Test
  void ensureBioNotBlank() {
    assertThrows(IllegalArgumentException.class, () -> new Author(validName, " ", null));
  }

  @Test
  void ensureAuthorIsCreatedWithNameAndBio() {
    Author author = new Author(validName, validBio, null);

    assertEquals(validName, author.getName());
    assertEquals(validBio, author.getBio());
  }

  @Test
  void ensureNewAuthorHasInitialVersionAndNoId() {
    Author author = new Author(validName, validBio, null);

    assertEquals(0L, author.getVersion());
    assertNull(author.getId());
  }

  @Test
  void ensurePhotoCanBeNull_AkaOptional() {
    Author author = new Author(validName, validBio, null);
    assertNull(author.getPhoto());
  }

  @Test
  void ensureValidPhoto() {
    Author author = new Author(validName, validBio, "photoTest.jpg");
    Photo photo = author.getPhoto();
    assertNotNull(photo);
    assertEquals("photoTest.jpg", photo.getPhotoFile());
  }

  @Test
  void whenVersionIsStaleItIsNotPossibleToPatch() {
    Author author = new Author(validName, validBio, null);

    assertThrows(
        StaleObjectStateException.class, () -> author.applyPatch(999, "Outro", null, null));
    assertEquals(validName, author.getName());
  }

  @Test
  void ensurePatchUpdatesProvidedFields() {
    Author author = new Author(validName, validBio, null);

    author.applyPatch(0L, "Novo Nome", "Nova bio", "new.png");

    assertEquals("Novo Nome", author.getName());
    assertEquals("Nova bio", author.getBio());
    assertEquals("new.png", author.getPhoto().getPhotoFile());
  }

  @Test
  void ensurePatchKeepsFieldsThatAreNotProvided() {
    Author author = new Author(validName, validBio, "photoTest.jpg");

    author.applyPatch(0L, null, null, null);

    assertEquals(validName, author.getName());
    assertEquals(validBio, author.getBio());
    assertEquals("photoTest.jpg", author.getPhoto().getPhotoFile());
  }

  @Test
  void ensurePatchWithInvalidNameFails() {
    Author author = new Author(validName, validBio, null);

    assertThrows(IllegalArgumentException.class, () -> author.applyPatch(0L, "N@me", null, null));
    assertEquals(validName, author.getName());
  }

  @Test
  void ensurePatchWithBlankBioFails() {
    Author author = new Author(validName, validBio, null);

    assertThrows(IllegalArgumentException.class, () -> author.applyPatch(0L, null, "", null));
    assertEquals(validBio, author.getBio());
  }

  @Test
  void ensureRemovePhotoWithWrongVersionFails() {
    Author author = new Author(validName, validBio, "photoTest.jpg");

    assertThrows(ConflictException.class, () -> author.removePhoto(1L));
    assertNotNull(author.getPhoto());
  }

  @Test
  void ensureRemovePhotoClearsPhoto() {
    Author author = new Author(validName, validBio, "photoTest.jpg");

    author.removePhoto(0L);

    assertNull(author.getPhoto());
  }
}
