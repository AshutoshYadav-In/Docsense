package com.project.ashutosh.tenant;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Thread-local holder for the current request's tenant after {@link
 * com.project.ashutosh.security.TenantResolutionFilter} validates {@code X-Tenant-Id}. Use {@link
 * #getTenantId()} in services that scope data by tenant.
 */
@Component
public class TenantContext {

  private static final ThreadLocal<Context> HOLDER = new ThreadLocal<>();

  public void set(Long internalTenantId, UUID referenceId, String tenantName) {
    HOLDER.set(new Context(internalTenantId, referenceId, tenantName));
  }

  public Optional<Context> get() {
    return Optional.ofNullable(HOLDER.get());
  }

  public Long getTenantId() {
    return get().map(Context::internalTenantId).orElse(null);
  }

  public void clear() {
    HOLDER.remove();
  }

  public record Context(Long internalTenantId, UUID referenceId, String tenantName) {}
}
