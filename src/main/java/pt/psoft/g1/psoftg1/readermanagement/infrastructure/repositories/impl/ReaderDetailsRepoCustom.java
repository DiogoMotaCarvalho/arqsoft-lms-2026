package pt.psoft.g1.psoftg1.readermanagement.infrastructure.repositories.impl;

import java.util.List;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.SearchReadersQuery;

interface ReaderDetailsRepoCustom {

  List<ReaderDetails> searchReaderDetails(
      pt.psoft.g1.psoftg1.shared.services.Page page, SearchReadersQuery query);
}
