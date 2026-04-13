package com.project.ashutosh.dao;

import com.project.ashutosh.entity.Tenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TenantDao extends SimpleJpaRepository<Tenant, Long> {

  private static final String REFERENCE_ID = "referenceId";

  private final EntityManager entityManager;

  public TenantDao(EntityManager entityManager) {
    super(JpaEntityInformationSupport.getEntityInformation(Tenant.class, entityManager), entityManager);
    this.entityManager = entityManager;
  }

  public Optional<Tenant> findByReferenceId(UUID tenantReferenceId) {
    EntityManager em = entityManager;
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Tenant> cq = cb.createQuery(Tenant.class);
    Root<Tenant> root = cq.from(Tenant.class);
    cq.select(root).where(cb.equal(root.get(REFERENCE_ID), tenantReferenceId));
    return em.createQuery(cq).setMaxResults(1).getResultStream().findFirst();
  }
}
