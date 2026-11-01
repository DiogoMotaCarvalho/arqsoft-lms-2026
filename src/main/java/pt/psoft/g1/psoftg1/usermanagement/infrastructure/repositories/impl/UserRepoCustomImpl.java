package pt.psoft.g1.psoftg1.usermanagement.infrastructure.repositories.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.services.SearchUsersQuery;

@RequiredArgsConstructor
class UserRepoCustomImpl implements UserRepoCustom {

  // get the underlying JPA Entity Manager via spring thru constructor dependency
  // injection
  private final EntityManager em;

  @Override
  public List<User> searchUsers(final Page page, final SearchUsersQuery query) {

    final CriteriaBuilder cb = em.getCriteriaBuilder();
    final CriteriaQuery<User> cq = cb.createQuery(User.class);
    final Root<User> root = cq.from(User.class);
    cq.select(root);

    final List<Predicate> where = new ArrayList<>();
    if (StringUtils.hasText(query.getUsername())) {
      where.add(cb.equal(root.get("username"), query.getUsername()));
    }
    if (StringUtils.hasText(query.getFullName())) {
      where.add(cb.like(root.get("fullName"), "%" + query.getFullName() + "%"));
    }

    // search using OR
    if (!where.isEmpty()) {
      cq.where(cb.or(where.toArray(new Predicate[0])));
    }

    cq.orderBy(cb.desc(root.get("createdAt")));

    final TypedQuery<User> q = em.createQuery(cq);
    q.setFirstResult((page.getNumber() - 1) * page.getLimit());
    q.setMaxResults(page.getLimit());

    return q.getResultList();
  }
}
