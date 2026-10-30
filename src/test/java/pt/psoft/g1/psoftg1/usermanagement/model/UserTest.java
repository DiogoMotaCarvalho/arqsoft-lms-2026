package pt.psoft.g1.psoftg1.usermanagement.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UserTest {
  private static final String USERNAME = "manuel@gmail.com";
  private static final String PASSWORD = "Manuelino123!";

  @Test
  void ensurePasswordIsStoredEncoded() {
    User user = new User(USERNAME, PASSWORD);

    assertNotEquals(PASSWORD, user.getPassword());
    assertTrue(new BCryptPasswordEncoder().matches(PASSWORD, user.getPassword()));
  }

  @Test
  void ensureWeakPasswordIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new User(USERNAME, "weak"));
  }

  @Test
  void ensureNewUserSetsName() {
    User user = User.newUser(USERNAME, PASSWORD, "Manuel Coives");

    assertEquals("Manuel Coives", user.getName().toString());
    assertTrue(user.getAuthorities().isEmpty());
  }

  @Test
  void ensureNewUserWithRoleAddsAuthority() {
    User user = User.newUser(USERNAME, PASSWORD, "Manuel Coives", Role.ADMIN);

    assertEquals(1, user.getAuthorities().size());
    assertTrue(user.getAuthorities().contains(new Role(Role.ADMIN)));
  }

  @Test
  void ensureInvalidNameIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> User.newUser(USERNAME, PASSWORD, "M@nuel"));
  }

  @Test
  void ensureReaderHasReaderRole() {
    Reader reader = Reader.newReader(USERNAME, PASSWORD, "Manuel Coives");

    assertEquals(USERNAME, reader.getUsername());
    assertEquals("Manuel Coives", reader.getName().toString());
    assertTrue(reader.getAuthorities().contains(new Role(Role.READER)));
  }

  @Test
  void ensureLibrarianHasLibrarianRole() {
    Librarian librarian = Librarian.newLibrarian(USERNAME, PASSWORD, "Maria Silva");

    assertTrue(librarian.getAuthorities().contains(new Role(Role.LIBRARIAN)));
    assertFalse(librarian.getAuthorities().contains(new Role(Role.READER)));
  }

  @Test
  void ensureAccountFlagsFollowEnabledState() {
    User user = new User(USERNAME, PASSWORD);
    assertTrue(user.isEnabled());
    assertTrue(user.isAccountNonExpired());
    assertTrue(user.isAccountNonLocked());
    assertTrue(user.isCredentialsNonExpired());

    user.setEnabled(false);

    assertFalse(user.isAccountNonExpired());
    assertFalse(user.isAccountNonLocked());
    assertFalse(user.isCredentialsNonExpired());
  }

  @Test
  void ensureRolesWithSameAuthorityAreEqual() {
    assertEquals(new Role(Role.READER), new Role(Role.READER));
    assertEquals(Role.READER, new Role(Role.READER).getAuthority());
  }

  @Test
  void ensureChangedPasswordIsStoredEncoded() {
    User user = new User(USERNAME, PASSWORD);

    user.setPassword("OutraPass456!");

    assertTrue(new BCryptPasswordEncoder().matches("OutraPass456!", user.getPassword()));
    assertFalse(new BCryptPasswordEncoder().matches(PASSWORD, user.getPassword()));
  }

  @Test
  void ensureRejectedPasswordChangeKeepsPreviousPassword() {
    User user = new User(USERNAME, PASSWORD);
    String encoded = user.getPassword();

    assertThrows(IllegalArgumentException.class, () -> user.setPassword("weak"));
    assertEquals(encoded, user.getPassword());
  }

  @Test
  void ensureAuthoritiesAreNotDuplicated() {
    Reader reader = Reader.newReader(USERNAME, PASSWORD, "Manuel Coives");

    reader.addAuthority(new Role(Role.READER));

    assertEquals(1, reader.getAuthorities().size());
  }
}
