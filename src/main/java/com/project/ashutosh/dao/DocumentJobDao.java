package com.project.ashutosh.dao;

import com.project.ashutosh.entity.DocumentJob;
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
public class DocumentJobDao extends SimpleJpaRepository<DocumentJob, Long> {

  private static final String REFERENCE_ID = "referenceId";

  private final EntityManager entityManager;

  public DocumentJobDao(EntityManager entityManager) {
    super(
        JpaEntityInformationSupport.getEntityInformation(DocumentJob.class, entityManager),
        entityManager);
    this.entityManager = entityManager;
  }

  public Optional<DocumentJob> findByReferenceId(UUID referenceId) {
    EntityManager em = entityManager;
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<DocumentJob> cq = cb.createQuery(DocumentJob.class);
    Root<DocumentJob> root = cq.from(DocumentJob.class);
    cq.select(root).where(cb.equal(root.get(REFERENCE_ID), referenceId));
    return em.createQuery(cq).setMaxResults(1).getResultStream().findFirst();
  }
}
