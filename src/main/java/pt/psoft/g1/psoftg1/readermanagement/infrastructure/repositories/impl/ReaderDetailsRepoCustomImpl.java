package pt.psoft.g1.psoftg1.readermanagement.infrastructure.repositories.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.SearchReadersQuery;
import pt.psoft.g1.psoftg1.usermanagement.model.User;

@RequiredArgsConstructor
class ReaderDetailsRepoCustomImpl implements ReaderDetailsRepoCustom {

  private final EntityManager em;

  @Override
  public List<ReaderDetails> searchReaderDetails(
      final pt.psoft.g1.psoftg1.shared.services.Page page, final SearchReadersQuery query) {

    final CriteriaBuilder cb = em.getCriteriaBuilder();
    final CriteriaQuery<ReaderDetails> cq = cb.createQuery(ReaderDetails.class);
    final Root<ReaderDetails> readerDetailsRoot = cq.from(ReaderDetails.class);
    Join<ReaderDetails, User> userJoin = readerDetailsRoot.join("reader");

    cq.select(readerDetailsRoot);

    final List<Predicate> where = new ArrayList<>();
    if (StringUtils.hasText(query.getName())) { // 'contains' type search
      where.add(cb.like(userJoin.get("name").get("name"), "%" + query.getName() + "%"));
      cq.orderBy(cb.asc(userJoin.get("name")));
    }
    if (StringUtils.hasText(query.getEmail())) { // 'exatct' type search
      where.add(cb.equal(userJoin.get("username"), query.getEmail()));
      cq.orderBy(cb.asc(userJoin.get("username")));
    }
    if (StringUtils.hasText(query.getPhoneNumber())) { // 'exatct' type search
      where.add(
          cb.equal(
              readerDetailsRoot.get("phoneNumber").get("phoneNumber"), query.getPhoneNumber()));
      cq.orderBy(cb.asc(readerDetailsRoot.get("phoneNumber").get("phoneNumber")));
    }

    // search using OR
    if (!where.isEmpty()) {
      cq.where(cb.or(where.toArray(new Predicate[0])));
    }

    final TypedQuery<ReaderDetails> q = em.createQuery(cq);
    q.setFirstResult((page.getNumber() - 1) * page.getLimit());
    q.setMaxResults(page.getLimit());

    return q.getResultList();
  }
}
