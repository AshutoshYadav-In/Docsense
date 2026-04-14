package com.project.ashutosh.security;

import ch.qos.logback.core.util.StringUtil;
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
 *
 * <p>HTTP errors are resolved here (not in services): {@code 401} if not authenticated, {@code 400}
 * if header missing/invalid UUID, {@code 404} if tenant unknown, {@code 403} if user is not a
 * member.
 *
 * <p>Excluded (no X-Tenant-Id): {@code POST /api/tenants} (create), {@code GET /api/tenants} (list
 * mine). All other {@code /api/tenants/...} and {@code /api/users} paths require the header.
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
      if (StringUtil.isNullOrEmpty(header)) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing X-Tenant-Id");
        return;
      }
      UUID referenceId;
      try {
        referenceId = UUID.fromString(header);
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

  static String normalizedPath(HttpServletRequest request) {
    String uri = request.getRequestURI();
    String context = request.getContextPath();
    if (context != null && !context.isEmpty() && uri.startsWith(context)) {
      uri = uri.substring(context.length());
    }
    if (uri.length() > 1 && uri.endsWith("/")) {
      uri = uri.substring(0, uri.length() - 1);
    }
    return uri.isEmpty() ? "/" : uri;
  }

  /** True when this request must carry X-Tenant-Id and the user must be a member. */
  static boolean tenantApiPath(HttpServletRequest request) {
    String path = normalizedPath(request);
    String method = request.getMethod();
    if ("/api/tenants".equals(path)) {
      if ("POST".equalsIgnoreCase(method) || "GET".equalsIgnoreCase(method)) {
        return false;
      }
    }
    if (path.startsWith("/api/users")) {
      return true;
    }
    return path.startsWith("/api/tenants/");
  }
}
