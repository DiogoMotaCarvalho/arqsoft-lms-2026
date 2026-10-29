package pt.psoft.g1.psoftg1.bookmanagement.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Transparent-box tests over the construction of the neutral bibliographic record (issue #2,
 * ADR-0008). Each case asserts the outcome of a construction branch: a required field absent or blank
 * fails fast, and an absent optional field is handled gracefully. This is the mapping that #2 owns;
 * source-specific parsing is tested in the adapters (#3, #4).
 */
class BibliographicDataTest {

  private static final String ISBN = "9782826012092";
  private static final String TITLE = "O Inspetor Max";

  @Test
  void buildsWithAllFieldsPresent() {
    BibliographicData data =
        new BibliographicData(ISBN, TITLE, "A description", List.of("Ana", "Bruno"), "google-books");

    assertThat(data.getIsbn()).isEqualTo(ISBN);
    assertThat(data.getTitle()).isEqualTo(TITLE);
    assertThat(data.getDescription()).isEqualTo("A description");
    assertThat(data.getAuthorNames()).containsExactly("Ana", "Bruno");
    assertThat(data.getSource()).isEqualTo("google-books");
  }

  // --- required fields fail fast ---

  @Test
  void failsFastWhenIsbnIsNull() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new BibliographicData(null, TITLE, null, null, "s"))
        .withMessageContaining("isbn");
  }

  @Test
  void failsFastWhenIsbnIsBlank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new BibliographicData("   ", TITLE, null, null, "s"))
        .withMessageContaining("isbn");
  }

  @Test
  void failsFastWhenTitleIsNull() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new BibliographicData(ISBN, null, null, null, "s"))
        .withMessageContaining("title");
  }

  @Test
  void failsFastWhenTitleIsBlank() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> new BibliographicData(ISBN, " ", null, null, "s"))
        .withMessageContaining("title");
  }

  // --- optional fields handled gracefully ---

  @Test
  void absentDescriptionIsNull() {
    BibliographicData data = new BibliographicData(ISBN, TITLE, null, List.of("Ana"), "s");
    assertThat(data.getDescription()).isNull();
  }

  @Test
  void absentAuthorListBecomesEmptyRatherThanNull() {
    BibliographicData data = new BibliographicData(ISBN, TITLE, null, null, "s");
    assertThat(data.getAuthorNames()).isNotNull().isEmpty();
  }

  @Test
  void authorNamesAreCopiedSoLaterMutationOfTheSourceListDoesNotLeakIn() {
    List<String> source = new ArrayList<>(List.of("Ana"));
    BibliographicData data = new BibliographicData(ISBN, TITLE, null, source, "s");

    source.add("Bruno"); // mutate the caller's list after construction

    assertThat(data.getAuthorNames()).containsExactly("Ana");
  }

  @Test
  void requiredFieldsPresentWithEmptyOptionalsConstructsSuccessfully() {
    assertThatNoException()
        .isThrownBy(() -> new BibliographicData(ISBN, TITLE, null, List.of(), null));
  }
}
