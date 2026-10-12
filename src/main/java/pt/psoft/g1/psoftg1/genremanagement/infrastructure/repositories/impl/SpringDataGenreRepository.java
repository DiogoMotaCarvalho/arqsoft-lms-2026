package pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.impl;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDto;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;

public interface SpringDataGenreRepository
    extends GenreRepository, GenreRepoCustom, CrudRepository<Genre, Integer> {

  @Query("SELECT g FROM Genre g")
  List<Genre> findAllGenres();

  @Override
  @Query("SELECT g FROM Genre g WHERE g.genre = :genreName")
  Optional<Genre> findByString(@Param("genreName") @NotNull String genre);

  @Override
  @Query(
      "SELECT new pt.psoft.g1.psoftg1.bookmanagement.services.GenreBookCountDto(g.genre, COUNT(b))"
          + "FROM Genre g "
          + "JOIN Book b ON b.genre.pk = g.pk "
          + "GROUP BY g "
          + "ORDER BY COUNT(b) DESC")
  Page<GenreBookCountDto> findTop5GenreByBookCount(Pageable pageable);
}
