package pt.psoft.g1.psoftg1.bookmanagement.services;

import java.util.List;

class BibliographicProviderContractTest extends BibliographicProviderContract {

  @Override
  protected BibliographicProvider providerThatKnows(String isbn, String title) {
    return new InMemoryBibliographicProvider()
        .with(new BibliographicData(isbn, title, "A description", List.of("Ana"), "stub"));
  }

  @Override
  protected BibliographicProvider providerThatKnowsNothing() {
    return new InMemoryBibliographicProvider();
  }

  @Override
  protected BibliographicProvider providerThatFails() {
    return new InMemoryBibliographicProvider().failingFor(KNOWN_ISBN, "source unreachable");
  }
}
