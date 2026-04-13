package com.project.ashutosh.dao;

import com.project.ashutosh.entity.TenantMember;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class TenantMemberDaoImpl implements TenantMemberDaoCustom {

  @PersistenceContext
  private EntityManager em;

  @Override
  public boolean existsMembership(Long tenantInternalId, Long userId) {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<TenantMember> root = cq.from(TenantMember.class);
    cq.select(cb.count(root)).where(
        cb.and(cb.equal(root.get("tenant").get("id"), tenantInternalId), cb.equal(root.get("user").get("id"), userId)));
    Long count = em.createQuery(cq).getSingleResult();
    return count != null && count > 0;
  }
}
