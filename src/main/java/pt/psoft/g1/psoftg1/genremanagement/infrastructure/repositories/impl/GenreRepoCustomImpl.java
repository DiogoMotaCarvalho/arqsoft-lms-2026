package pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsDto;
import pt.psoft.g1.psoftg1.genremanagement.services.GenreLendingsPerMonthDto;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;

@RequiredArgsConstructor
class GenreRepoCustomImpl implements GenreRepoCustom {

  private final EntityManager entityManager;

  @Override
  public List<GenreLendingsPerMonthDto> getLendingsPerMonthLastYearByGenre() {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
    Root<Lending> lendingRoot = cq.from(Lending.class);
    Join<Lending, Book> bookJoin = lendingRoot.join("book");
    Join<Book, Genre> genreJoin = bookJoin.join("genre");

    Expression<Integer> year = cb.function("YEAR", Integer.class, lendingRoot.get("startDate"));
    Expression<Integer> month = cb.function("MONTH", Integer.class, lendingRoot.get("startDate"));

    Expression<Long> lendingCount = cb.count(lendingRoot);

    cq.multiselect(genreJoin.get("genre"), year, month, lendingCount);
    cq.groupBy(genreJoin.get("genre"), year, month);

    // Predicate to filter the last 12 months
    LocalDate now = LocalDate.now();
    LocalDate twelveMonthsAgo = now.minusMonths(12);
    Predicate datePredicate =
        cb.between(
            lendingRoot.get("startDate"),
            java.sql.Date.valueOf(twelveMonthsAgo),
            java.sql.Date.valueOf(now));

    cq.where(datePredicate);
    cq.orderBy(cb.asc(year), cb.asc(month), cb.asc(genreJoin.get("genre")));

    TypedQuery<Tuple> query = entityManager.createQuery(cq);
    List<Tuple> results = query.getResultList();

    // Grouping results by year and month
    Map<Integer, Map<Integer, List<GenreLendingsDto>>> groupedResults = new HashMap<>();

    for (Tuple result : results) {
      String genre = result.get(0, String.class);
      int resultYear = result.get(1, Integer.class);
      int resultMonth = result.get(2, Integer.class);
      long count = result.get(3, Long.class);
      GenreLendingsDto genreLendingsDto = new GenreLendingsDto(genre, count);

      groupedResults
          .computeIfAbsent(resultYear, k -> new HashMap<>())
          .computeIfAbsent(resultMonth, k -> new ArrayList<>())
          .add(genreLendingsDto);
    }

    return getGenreLendingsPerMonthDtos(groupedResults);
  }

  @Override
  public List<GenreLendingsDto> getAverageLendingsInMonth(
      LocalDate month, pt.psoft.g1.psoftg1.shared.services.Page page) {
    int days = month.lengthOfMonth();
    LocalDate firstOfMonth = LocalDate.of(month.getYear(), month.getMonth(), 1);
    LocalDate lastOfMonth = LocalDate.of(month.getYear(), month.getMonth(), days);

    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<GenreLendingsDto> cq = cb.createQuery(GenreLendingsDto.class);

    Root<Lending> lendingRoot = cq.from(Lending.class);
    Join<Lending, Book> bookJoin = lendingRoot.join("book", JoinType.LEFT);
    Join<Book, Genre> genreJoin = bookJoin.join("genre", JoinType.LEFT);

    Expression<Long> loanCount = cb.count(lendingRoot.get("pk"));
    Expression<Number> dailyAvgLoans = cb.quot(cb.toDouble(loanCount), cb.literal(days));

    cq.multiselect(genreJoin, dailyAvgLoans);
    cq.groupBy(genreJoin.get("pk"));

    Predicate startDatePredicate =
        cb.greaterThanOrEqualTo(lendingRoot.get("startDate"), firstOfMonth);
    Predicate endDatePredicate = cb.lessThanOrEqualTo(lendingRoot.get("startDate"), lastOfMonth);

    Predicate finalPredicate = cb.and(startDatePredicate, endDatePredicate);

    cq.where(finalPredicate);

    final TypedQuery<GenreLendingsDto> q = entityManager.createQuery(cq);
    q.setFirstResult((page.getNumber() - 1) * page.getLimit());
    q.setMaxResults(page.getLimit());

    return q.getResultList();
  }

  @Override
  public List<GenreLendingsPerMonthDto> getLendingsAverageDurationPerMonth(
      LocalDate startDate, LocalDate endDate) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Tuple> cq = cb.createTupleQuery();

    Root<Lending> lendingRoot = cq.from(Lending.class);
    Join<Lending, Book> bookJoin = lendingRoot.join("book");
    Join<Book, Genre> genreJoin = bookJoin.join("genre");

    Expression<Integer> yearExpr = cb.function("YEAR", Integer.class, lendingRoot.get("startDate"));
    Expression<Integer> monthExpr =
        cb.function("MONTH", Integer.class, lendingRoot.get("startDate"));
    Expression<Long> lendingDurationInDays =
        cb.diff(lendingRoot.get("returnedDate"), lendingRoot.get("startDate"));
    double nanoSecondsPerDay = 86400.0 * 1E9;
    Expression<Number> durationInDays =
        cb.quot(cb.toDouble(lendingDurationInDays), nanoSecondsPerDay);
    Expression<Double> averageDuration = cb.avg(cb.toDouble(durationInDays));

    cq.multiselect(genreJoin.get("genre"), yearExpr, monthExpr, averageDuration);
    cq.groupBy(genreJoin.get("genre"), yearExpr, monthExpr);
    cq.orderBy(cb.asc(yearExpr), cb.asc(monthExpr), cb.asc(genreJoin.get("genre")));

    Predicate startDatePredicate = cb.greaterThanOrEqualTo(lendingRoot.get("startDate"), startDate);
    Predicate endDatePredicate = cb.lessThanOrEqualTo(lendingRoot.get("startDate"), endDate);
    Predicate returnedPredicate = cb.isNotNull(lendingRoot.get("returnedDate"));
    Predicate finalPredicate = cb.and(startDatePredicate, endDatePredicate, returnedPredicate);

    cq.where(finalPredicate);

    List<Tuple> results = entityManager.createQuery(cq).getResultList();
    Map<Integer, Map<Integer, List<GenreLendingsDto>>> groupedResults = new HashMap<>();

    for (Tuple result : results) {
      String genre = result.get(0, String.class);
      int yearValue = result.get(1, Integer.class);
      int monthValue = result.get(2, Integer.class);
      Double averageDurationValue = result.get(3, Double.class);
      GenreLendingsDto genreLendingsDto = new GenreLendingsDto(genre, averageDurationValue);

      groupedResults
          .computeIfAbsent(yearValue, k -> new HashMap<>())
          .computeIfAbsent(monthValue, k -> new ArrayList<>())
          .add(genreLendingsDto);
    }

    return getGenreLendingsPerMonthDtos(groupedResults);
  }

  @NotNull
  private List<GenreLendingsPerMonthDto> getGenreLendingsPerMonthDtos(
      Map<Integer, Map<Integer, List<GenreLendingsDto>>> groupedResults) {
    List<GenreLendingsPerMonthDto> lendingsPerMonth = new ArrayList<>();
    for (Map.Entry<Integer, Map<Integer, List<GenreLendingsDto>>> yearEntry :
        groupedResults.entrySet()) {
      int yearValue = yearEntry.getKey();
      for (Map.Entry<Integer, List<GenreLendingsDto>> monthEntry :
          yearEntry.getValue().entrySet()) {
        int monthValue = monthEntry.getKey();
        List<GenreLendingsDto> values = monthEntry.getValue();
        lendingsPerMonth.add(new GenreLendingsPerMonthDto(yearValue, monthValue, values));
      }
    }

    return lendingsPerMonth;
  }
}
