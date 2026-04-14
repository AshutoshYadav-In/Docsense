package com.project.ashutosh.service;

import com.project.ashutosh.dao.TenantDao;
import com.project.ashutosh.dao.TenantMemberDao;
import com.project.ashutosh.entity.Tenant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TenantMembershipService {

  private final TenantDao tenantDao;
  private final TenantMemberDao tenantMemberDao;

  public TenantMembershipService(TenantDao tenantDao, TenantMemberDao tenantMemberDao) {
    this.tenantDao = tenantDao;
    this.tenantMemberDao = tenantMemberDao;
  }

  public Optional<Tenant> findByReferenceId(UUID tenantReferenceId) {
    return tenantDao.findByReferenceId(tenantReferenceId);
  }

  public boolean isMember(Long userId, Long tenantInternalId) {
    return tenantMemberDao.existsMembership(tenantInternalId, userId);
  }
}
