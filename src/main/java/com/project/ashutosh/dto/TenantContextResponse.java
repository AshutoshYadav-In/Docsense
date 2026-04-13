package com.project.ashutosh.dto;

import com.project.ashutosh.tenant.TenantContext;
import java.util.UUID;

/**
 * Current tenant for the request. {@code tenantId} is the internal DB primary key; {@code
 * referenceId} is the external id sent in {@code X-Tenant-Id}.
 */
public record TenantContextResponse(Long tenantId, UUID referenceId, String tenantName) {

  public static TenantContextResponse fromContext(TenantContext.Context ctx) {
    return new TenantContextResponse(ctx.internalTenantId(), ctx.referenceId(), ctx.tenantName());
  }
}
