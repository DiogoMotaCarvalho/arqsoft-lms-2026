package pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.shared.services.Page;

@RequiredArgsConstructor
class LendingRepoCustomImpl implements LendingRepoCustom {
  // get the underlying JPA Entity Manager via spring thru constructor dependency
  // injection
  private final EntityManager em;

  @Override
  public List<Lending> getOverdue(Page page) {

    final CriteriaBuilder cb = em.getCriteriaBuilder();
    final CriteriaQuery<Lending> cq = cb.createQuery(Lending.class);
    final Root<Lending> root = cq.from(Lending.class);
    cq.select(root);

    final List<Predicate> where = new ArrayList<>();

    // Select overdue lendings where returnedDate is null and limitDate is before the current date
    where.add(cb.isNull(root.get("returnedDate")));
    where.add(cb.lessThan(root.get("limitDate"), LocalDate.now()));

    cq.where(where.toArray(new Predicate[0]));
    cq.orderBy(cb.asc(root.get("limitDate"))); // Order by limitDate, oldest first

    final TypedQuery<Lending> q = em.createQuery(cq);
    q.setFirstResult((page.getNumber() - 1) * page.getLimit());
    q.setMaxResults(page.getLimit());

    return q.getResultList();
  }

  public List<Lending> searchLendings(
      Page page,
      String readerNumber,
      String isbn,
      Boolean returned,
      LocalDate startDate,
      LocalDate endDate) {
    final CriteriaBuilder cb = em.getCriteriaBuilder();
    final CriteriaQuery<Lending> cq = cb.createQuery(Lending.class);
    final Root<Lending> lendingRoot = cq.from(Lending.class);
    final Join<Lending, Book> bookJoin = lendingRoot.join("book");
    final Join<Lending, ReaderDetails> readerDetailsJoin = lendingRoot.join("readerDetails");
    cq.select(lendingRoot);

    final List<Predicate> where = new ArrayList<>();

    if (StringUtils.hasText(readerNumber)) {
      where.add(cb.like(readerDetailsJoin.get("readerNumber").get("readerNumber"), readerNumber));
    }
    if (StringUtils.hasText(isbn)) {
      where.add(cb.like(bookJoin.get("isbn").get("isbn"), isbn));
    }
    if (returned != null) {
      if (returned) {
        where.add(cb.isNotNull(lendingRoot.get("returnedDate")));
      } else {
        where.add(cb.isNull(lendingRoot.get("returnedDate")));
      }
    }
    if (startDate != null) {
      where.add(cb.greaterThanOrEqualTo(lendingRoot.get("startDate"), startDate));
    }
    if (endDate != null) {
      where.add(cb.lessThanOrEqualTo(lendingRoot.get("startDate"), endDate));
    }

    cq.where(where.toArray(new Predicate[0]));
    cq.orderBy(cb.asc(lendingRoot.get("lendingNumber")));

    final TypedQuery<Lending> q = em.createQuery(cq);
    q.setFirstResult((page.getNumber() - 1) * page.getLimit());
    q.setMaxResults(page.getLimit());

    return q.getResultList();
  }
}
