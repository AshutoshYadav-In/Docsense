package com.project.ashutosh.dao;

/** Custom persistence for tenant membership using JPA Criteria API only (see {@link TenantMemberDaoImpl}). */
public interface TenantMemberDaoCustom {

  boolean existsMembership(Long tenantInternalId, Long userId);
}
