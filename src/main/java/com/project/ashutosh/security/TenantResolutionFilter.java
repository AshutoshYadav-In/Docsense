package com.project.ashutosh.security;

import com.project.ashutosh.entity.Tenant;
import com.project.ashutosh.service.TenantMembershipService;
import com.project.ashutosh.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * After JWT authentication, validates {@code X-Tenant-Id} (reference id) and membership for
 * tenant-scoped API paths. Sets {@link TenantContext} for the request.
 */
public class TenantResolutionFilter extends OncePerRequestFilter {

  public static final String TENANT_HEADER = "X-Tenant-Id";

  private final TenantContext tenantContext;
  private final TenantMembershipService tenantMembershipService;

  public TenantResolutionFilter(
      TenantContext tenantContext, TenantMembershipService tenantMembershipService) {
    this.tenantContext = tenantContext;
    this.tenantMembershipService = tenantMembershipService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      if (!tenantApiPath(request)) {
        filterChain.doFilter(request, response);
        return;
      }
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth == null || !auth.isAuthenticated()) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
        return;
      }
      String header = request.getHeader(TENANT_HEADER);
      if (header == null || header.isBlank()) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing X-Tenant-Id");
        return;
      }
      UUID referenceId;
      try {
        referenceId = UUID.fromString(header.trim());
      } catch (IllegalArgumentException e) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Tenant-Id");
        return;
      }
      Long userId = Long.parseLong(auth.getName());
      Optional<Tenant> tenantOpt = tenantMembershipService.findByReferenceId(referenceId);
      if (tenantOpt.isEmpty()) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Tenant not found");
        return;
      }
      Tenant tenant = tenantOpt.get();
      if (!tenantMembershipService.isMember(userId, tenant.getId())) {
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not a member of this tenant");
        return;
      }
      tenantContext.set(tenant.getId(), tenant.getReferenceId(), tenant.getName());
      filterChain.doFilter(request, response);
    } finally {
      tenantContext.clear();
    }
  }

  /** Paths under {@code /api/users} after context path. */
  private static boolean tenantApiPath(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String context = request.getContextPath();
    if (context != null && !context.isEmpty() && uri.startsWith(context)) {
      uri = uri.substring(context.length());
    }
    return uri.startsWith("/api/users");
  }
}
