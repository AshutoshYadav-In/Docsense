package com.project.ashutosh.dao;

import com.project.ashutosh.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
public class UserDao extends SimpleJpaRepository<User, Long> {

  private static final String EMAIL = "email";

  private final EntityManager entityManager;

  public UserDao(EntityManager entityManager) {
    super(JpaEntityInformationSupport.getEntityInformation(User.class, entityManager), entityManager);
    this.entityManager = entityManager;
  }

  public Optional<User> findByEmail(String email) {
    EntityManager em = entityManager;
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<User> cq = cb.createQuery(User.class);
    Root<User> root = cq.from(User.class);
    cq.select(root).where(cb.equal(root.get(EMAIL), email));
    return em.createQuery(cq).setMaxResults(1).getResultStream().findFirst();
  }

  public List<User> findByEmailIn(Collection<String> emails) {
    if (CollectionUtils.isEmpty(emails)) {
      return Collections.emptyList();
    }
    EntityManager em = entityManager;
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<User> cq = cb.createQuery(User.class);
    Root<User> root = cq.from(User.class);
    cq.select(root).where(root.get(EMAIL).in(emails));
    return em.createQuery(cq).getResultList();
  }

  public Optional<User> findByEmailIgnoreCase(String email) {
    EntityManager em = entityManager;
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<User> cq = cb.createQuery(User.class);
    Root<User> root = cq.from(User.class);
    cq.select(root).where(cb.equal(cb.lower(root.get(EMAIL)), email == null ? null : email.toLowerCase()));
    return em.createQuery(cq).setMaxResults(1).getResultStream().findFirst();
  }

  public boolean existsByEmail(String email) {
    EntityManager em = entityManager;
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<User> root = cq.from(User.class);
    cq.select(cb.count(root)).where(cb.equal(root.get(EMAIL), email));
    Long count = em.createQuery(cq).getSingleResult();
    return count != null && count > 0;
  }
}
