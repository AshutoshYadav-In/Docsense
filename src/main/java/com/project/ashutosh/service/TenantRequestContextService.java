package com.project.ashutosh.service;

import com.project.ashutosh.dto.TenantContextResponse;
import com.project.ashutosh.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tenant-scoped request data. {@link com.project.ashutosh.security.TenantResolutionFilter} validates
 * {@code X-Tenant-Id} and membership before controllers; if context is missing here, it is a
 * server/configuration error, not a client mistake.
 */
@Service
public class TenantRequestContextService {

  private final TenantContext tenantContext;

  public TenantRequestContextService(TenantContext tenantContext) {
    this.tenantContext = tenantContext;
  }

  public TenantContextResponse requireTenantContextResponse() {
    return tenantContext
        .get()
        .map(TenantContextResponse::fromContext)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Tenant context missing after tenant filter"));
  }
}
