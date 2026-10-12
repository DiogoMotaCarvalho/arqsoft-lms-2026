package pt.psoft.g1.psoftg1.bookmanagement.services;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;

@Getter
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookCountDto {
  private Book book;
  private long lendingCount;
}
