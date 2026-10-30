package pt.psoft.g1.psoftg1.usermanagement.services;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.exceptions.NotFoundException;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.InMemoryForbiddenNameRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.InMemoryUserRepository;

class UserServiceTest {
  private static final String PASSWORD = "Manuelino123!";

  private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
  private final InMemoryForbiddenNameRepository forbiddenNameRepository =
      new InMemoryForbiddenNameRepository();

  private final UserService service =
      new UserService(
          userRepository,
          new EditUserMapperImpl(),
          forbiddenNameRepository,
          new BCryptPasswordEncoder());

  private static CreateUserRequest registration(String username, String name, String role) {
    CreateUserRequest request = new CreateUserRequest(username, name, PASSWORD);
    request.setRole(role);
    return request;
  }

  private User existingUser(String username) {
    return userRepository.save(Reader.newReader(username, PASSWORD, "Maria Silva"));
  }

  private static Authentication jwtFor(User user) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject(user.getId() + "," + user.getUsername())
            .build();
    return new JwtAuthenticationToken(jwt);
  }

  @Test
  void ensureReaderIsCreatedWithReaderRole() {
    User created = service.create(registration("manuel@gmail.com", "Manuel Coives", Role.READER));

    assertInstanceOf(Reader.class, created);
    assertEquals("Manuel Coives", created.getName().toString());
    assertTrue(created.getAuthorities().contains(new Role(Role.READER)));
    assertEquals(List.of(created), userRepository.findAll());
  }

  @Test
  void ensureLibrarianIsCreatedWithLibrarianRole() {
    User created = service.create(registration("maria@gmail.com", "Maria Silva", Role.LIBRARIAN));

    assertInstanceOf(Librarian.class, created);
    assertTrue(created.getAuthorities().contains(new Role(Role.LIBRARIAN)));
  }

  @Test
  void ensureUsernameMustBeUnique() {
    User existing = existingUser("maria@gmail.com");
    CreateUserRequest request = registration("maria@gmail.com", "Outra Maria", Role.READER);

    assertThrows(ConflictException.class, () -> service.create(request));
    assertEquals(List.of(existing), userRepository.findAll());
  }

  @Test
  void ensureNameCannotContainForbiddenWords() {
    forbiddenNameRepository.save(new ForbiddenName("Coiv"));
    CreateUserRequest request = registration("manuel@gmail.com", "Manuel Coives", Role.READER);

    assertThrows(IllegalArgumentException.class, () -> service.create(request));
    assertTrue(userRepository.findAll().isEmpty());
  }

  @Test
  void ensureUpdateChangesName() {
    User existing = existingUser("maria@gmail.com");

    User updated =
        service.update(existing.getId(), new EditUserRequest("Maria Santos", null, null, null));

    assertEquals("Maria Santos", updated.getName().toString());
  }

  @Test
  void ensureCannotUpdateUnknownUser() {
    EditUserRequest request = new EditUserRequest("Maria Santos", null, null, null);

    assertThrows(NotFoundException.class, () -> service.update(99L, request));
  }

  @Test
  void ensureDeletedUserIsDisabledAndNoLongerFound() {
    User existing = existingUser("maria@gmail.com");
    Long id = existing.getId();

    service.delete(id);

    assertFalse(existing.isEnabled());
    assertThrows(NotFoundException.class, () -> service.getUser(id));
  }

  @Test
  void ensureUserIsLoadedByUsername() {
    User existing = existingUser("maria@gmail.com");

    assertSame(existing, service.loadUserByUsername("maria@gmail.com"));
  }

  @Test
  void ensureLoadingUnknownUsernameFails() {
    assertThrows(
        UsernameNotFoundException.class, () -> service.loadUserByUsername("nobody@gmail.com"));
  }

  @Test
  void ensureAuthenticatedUserIsResolvedFromTokenSubject() {
    User existing = existingUser("maria@gmail.com");

    assertSame(existing, service.getAuthenticatedUser(jwtFor(existing)));
  }

  @Test
  void ensureMissingAuthenticationIsDenied() {
    assertThrows(AccessDeniedException.class, () -> service.getAuthenticatedUser(null));
  }

  @Test
  void ensureNonTokenAuthenticationIsDenied() {
    Authentication authentication = new UsernamePasswordAuthenticationToken("maria", null);

    assertThrows(AccessDeniedException.class, () -> service.getAuthenticatedUser(authentication));
  }

  @Test
  void ensureTokenOfRemovedUserIsDenied() {
    User existing = existingUser("maria@gmail.com");
    Authentication authentication = jwtFor(existing);
    userRepository.delete(existing);

    assertThrows(AccessDeniedException.class, () -> service.getAuthenticatedUser(authentication));
  }
}
