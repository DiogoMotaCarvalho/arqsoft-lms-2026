package pt.psoft.g1.psoftg1.shared.model;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PhotoTest {
  private static class PhotoHolder extends EntityWithPhoto {}

  @Test
  void ensurePathMustNotBeNull() {
    assertThrows(NullPointerException.class, () -> new Photo(null));
  }

  @Test
  void ensurePhotoStoresPath() {
    Photo photo = new Photo(Path.of("photoTest.jpg"));
    assertEquals("photoTest.jpg", photo.getPhotoFile());
  }

  @Test
  void ensureEntityStoresValidPhotoPath() {
    PhotoHolder entity = new PhotoHolder();

    entity.setPhoto("photoTest.jpg");

    assertEquals("photoTest.jpg", entity.getPhoto().getPhotoFile());
  }

  @Test
  void ensureEntityDiscardsInvalidPhotoPath() {
    PhotoHolder entity = new PhotoHolder();
    entity.setPhoto("photoTest.jpg");

    entity.setPhoto("invalid\0path.jpg");

    assertNull(entity.getPhoto());
  }

  @Test
  void ensureEntityPhotoCanBeCleared() {
    PhotoHolder entity = new PhotoHolder();
    entity.setPhoto("photoTest.jpg");

    entity.setPhoto(null);

    assertNull(entity.getPhoto());
  }
}
