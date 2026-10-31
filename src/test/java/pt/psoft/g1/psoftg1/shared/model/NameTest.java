package pt.psoft.g1.psoftg1.shared.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NameTest {
  @Test
  void ensureNameMustNotBeNull() {
    assertThrows(IllegalArgumentException.class, () -> new Name(null));
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "\t"})
  void ensureNameMustNotBeBlank(String name) {
    assertThrows(IllegalArgumentException.class, () -> new Name(name));
  }

  @ParameterizedTest
  @ValueSource(strings = {"Ricardo!", "joao@mail", "Ana.Silva", "<script>", "Rui_Costa"})
  void ensureNameMustOnlyBeAlphanumeric(String name) {
    assertThrows(IllegalArgumentException.class, () -> new Name(name));
  }

  @ParameterizedTest
  @ValueSource(strings = {"João Álvares", "Joana d'Arc", "Maria-José", "Pedro II", "Luís 3"})
  void ensureNameAcceptsAccentsApostrophesHyphensAndDigits(String name) {
    assertEquals(name, new Name(name).toString());
  }

  @Test
  void ensureNameIsSet() {
    final var name = new Name("Some name");
    assertEquals("Some name", name.toString());
  }

  @Test
  void ensureNameIsChanged() {
    final var name = new Name("Some name");
    name.setName("Some other name");
    assertEquals("Some other name", name.toString());
  }

  @Test
  void ensureInvalidChangeKeepsPreviousName() {
    final var name = new Name("Some name");

    assertThrows(IllegalArgumentException.class, () -> name.setName("N@me"));
    assertEquals("Some name", name.toString());
  }
}
