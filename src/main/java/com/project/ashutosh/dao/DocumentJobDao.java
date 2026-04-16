package com.project.ashutosh.dao;

import com.project.ashutosh.entity.DocumentJob;
import com.project.ashutosh.enums.DocumentJobStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class DocumentJobDao extends SimpleJpaRepository<DocumentJob, Long> {

  private static final String REFERENCE_ID = "referenceId";
  private static final String STATUS = "status";
  private static final String NUMBER_OF_CHUNKS = "numberOfChunks";
  private static final String UPDATED_AT = "updatedAt";

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

  public boolean existsByReferenceId(UUID referenceId) {
    return findByReferenceId(referenceId).isPresent();
  }

  /**
   * Criteria bulk update (does not run entity {@code @PreUpdate} callbacks). Sets {@code updated_at}
   * to the current instant inside this method.
   *
   * @return number of rows updated (0 if no matching {@code reference_id})
   */
  public int updateStatusByReferenceId(UUID referenceId, DocumentJobStatus status) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaUpdate<DocumentJob> update = cb.createCriteriaUpdate(DocumentJob.class);
    Root<DocumentJob> root = update.from(DocumentJob.class);
    update.set(root.get(STATUS), status);
    update.set(root.get(UPDATED_AT), Instant.now());
    update.where(cb.equal(root.get(REFERENCE_ID), referenceId));
    return entityManager.createQuery(update).executeUpdate();
  }

  public int updateCompletionByReferenceId(
      UUID referenceId, DocumentJobStatus status, int numberOfChunks) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaUpdate<DocumentJob> update = cb.createCriteriaUpdate(DocumentJob.class);
    Root<DocumentJob> root = update.from(DocumentJob.class);
    update.set(root.get(STATUS), status);
    update.set(root.get(NUMBER_OF_CHUNKS), numberOfChunks);
    update.set(root.get(UPDATED_AT), Instant.now());
    update.where(cb.equal(root.get(REFERENCE_ID), referenceId));
    return entityManager.createQuery(update).executeUpdate();
  }
}
