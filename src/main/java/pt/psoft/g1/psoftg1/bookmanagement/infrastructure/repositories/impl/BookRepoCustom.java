package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.repositories.impl;

import java.util.List;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.services.SearchBooksQuery;

interface BookRepoCustom {
  List<Book> searchBooks(pt.psoft.g1.psoftg1.shared.services.Page page, SearchBooksQuery query);
}
