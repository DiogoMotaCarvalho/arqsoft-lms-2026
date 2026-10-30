package pt.psoft.g1.psoftg1.shared.services;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ValidationException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;

class FileStorageServiceTest {
  private static final long MAX_SIZE = 100;

  @TempDir Path uploadDir;

  private FileStorageService service;

  @BeforeEach
  void setUp() {
    FileStorageProperties properties = new FileStorageProperties();
    properties.setUploadDir(uploadDir.toString());
    properties.setPhotoMaxSize(MAX_SIZE);
    service = new FileStorageService(properties);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  private static MockMultipartFile image(String name, String contentType, int size) {
    return new MockMultipartFile("photo", name, contentType, new byte[size]);
  }

  @Test
  void ensureStoreFileUsesPrefixAndOriginalExtension() throws Exception {
    MockMultipartFile file = new MockMultipartFile("f", "pic.png", "image/png", new byte[] {1, 2});

    String stored = service.storeFile("abc", file);

    assertEquals("abc.png", stored);
    assertArrayEquals(new byte[] {1, 2}, Files.readAllBytes(uploadDir.resolve("abc.png")));
  }

  @Test
  void ensureGetFileReturnsContentOrNull() throws Exception {
    Files.write(uploadDir.resolve("x.jpg"), new byte[] {7});

    assertArrayEquals(new byte[] {7}, service.getFile("x.jpg"));
    assertNull(service.getFile("missing.jpg"));
  }

  @Test
  void ensureDeleteFileRemovesExistingFile() throws Exception {
    Files.write(uploadDir.resolve("x.jpg"), new byte[] {7});

    service.deleteFile("x.jpg");

    assertFalse(Files.exists(uploadDir.resolve("x.jpg")));
  }

  @Test
  void ensureDeleteFileFailsForNullOrMissingFile() {
    assertThrows(IllegalArgumentException.class, () -> service.deleteFile(null));
    assertThrows(NotFoundException.class, () -> service.deleteFile("missing.jpg"));
  }

  @Test
  void ensureGetExtension() {
    assertEquals(Optional.of("png"), service.getExtension("a.b.png"));
    assertEquals(Optional.empty(), service.getExtension("noextension"));
    assertEquals(Optional.empty(), service.getExtension(null));
  }

  @Test
  void ensureLoadFileAsResource() throws Exception {
    Files.write(uploadDir.resolve("x.jpg"), new byte[] {7});

    Resource resource = service.loadFileAsResource("x.jpg");

    assertTrue(resource.exists());
    assertThrows(NotFoundException.class, () -> service.loadFileAsResource("missing.jpg"));
  }

  @Test
  void ensureGetRequestPhotoReturnsNullWithoutFile() {
    assertNull(service.getRequestPhoto(null));
  }

  @Test
  void ensureGetRequestPhotoRejectsOversizedFile() {
    MockMultipartFile tooBig = image("p.png", "image/png", (int) MAX_SIZE + 1);
    assertThrows(ValidationException.class, () -> service.getRequestPhoto(tooBig));
  }

  @Test
  void ensureGetRequestPhotoRejectsMissingContentType() {
    MockMultipartFile noType = image("p.png", null, 10);
    assertThrows(ValidationException.class, () -> service.getRequestPhoto(noType));
  }

  @Test
  void ensureGetRequestPhotoRejectsUnsupportedFormat() {
    MockMultipartFile gif = image("p.gif", "image/gif", 10);
    assertThrows(ValidationException.class, () -> service.getRequestPhoto(gif));
  }

  @Test
  void ensureGetRequestPhotoStoresValidImage() {
    RequestContextHolder.setRequestAttributes(
        new ServletRequestAttributes(new MockHttpServletRequest("POST", "/api/readers/photos")));

    String fileName = service.getRequestPhoto(image("p.jpg", "image/jpeg", 10));

    assertNotNull(fileName);
    assertTrue(fileName.endsWith(".jpg"));
    assertTrue(Files.exists(uploadDir.resolve(fileName)));
  }

  @Test
  void ensureGetRequestPhotoReturnsNullWhenUploadFails() {
    // no current HTTP request -> building the download URI fails and the error is swallowed
    assertNull(service.getRequestPhoto(image("p.png", "image/png", 10)));
  }
}
