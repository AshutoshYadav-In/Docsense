package com.project.ashutosh.dao;

import com.project.ashutosh.entity.Tenant;
import com.project.ashutosh.entity.TenantMember;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
public class TenantMemberDao extends SimpleJpaRepository<TenantMember, Long> {

  private static final String TENANT_ID = "tenantId";
  private static final String USER_ID = "userId";
  private static final String ID = "id";

  private final EntityManager entityManager;

  public TenantMemberDao(EntityManager entityManager) {
    super(
        JpaEntityInformationSupport.getEntityInformation(TenantMember.class, entityManager),
        entityManager);
    this.entityManager = entityManager;
  }

  public boolean existsMembership(Long tenantInternalId, Long userId) {
    EntityManager em = entityManager;
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<TenantMember> root = cq.from(TenantMember.class);
    cq.select(cb.count(root))
        .where(cb.and(cb.equal(root.get(TENANT_ID), tenantInternalId), cb.equal(root.get(USER_ID), userId)));
    Long count = em.createQuery(cq).getSingleResult();
    return count != null && count > 0;
  }

  public List<Tenant> findTenantsByUserId(Long userId) {
    EntityManager em = entityManager;
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Tenant> cq = cb.createQuery(Tenant.class);
    Root<TenantMember> membersRoot = cq.from(TenantMember.class);
    Root<Tenant> tenantRoot = cq.from(Tenant.class);
    cq.select(tenantRoot).distinct(true).where(
        cb.and(cb.equal(membersRoot.get(USER_ID), userId), cb.equal(membersRoot.get(TENANT_ID), tenantRoot.get(ID))));
    return em.createQuery(cq).getResultList();
  }

  public List<Long> findMemberUserIdsByTenantIdAndUserIdIn(Long tenantId, Collection<Long> userIds) {
    if (CollectionUtils.isEmpty(userIds)) {
      return Collections.emptyList();
    }
    EntityManager em = entityManager;
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<TenantMember> root = cq.from(TenantMember.class);
    cq.select(root.get(USER_ID)).distinct(true)
        .where(cb.and(cb.equal(root.get(TENANT_ID), tenantId), root.get(USER_ID).in(userIds)));
    return em.createQuery(cq).getResultList();
  }
}
