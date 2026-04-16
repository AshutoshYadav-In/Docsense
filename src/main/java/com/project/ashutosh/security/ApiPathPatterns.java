package com.project.ashutosh.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Single place for API path classification: JWT skip list, mandatory JWT under {@code /api/**}, and
 * when {@code X-Tenant-Id} is required. Used by {@link JwtAuthenticationFilter}, {@link
 * TenantResolutionFilter}, and {@link com.project.ashutosh.config.SecurityConfig} alignment.
 */
@Component
public class ApiPathPatterns {

  public String normalizedPath(HttpServletRequest request) {
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

  /** Paths where JWT is not used ({@code /api/auth/**}, {@code /api/internal/**}). */
  public boolean isJwtSkipped(String normalizedPath) {
    return isAuthPath(normalizedPath) || isInternalPath(normalizedPath);
  }

  public boolean isAuthPath(String normalizedPath) {
    return "/api/auth".equals(normalizedPath) || normalizedPath.startsWith("/api/auth/");
  }

  public boolean isInternalPath(String normalizedPath) {
    return "/api/internal".equals(normalizedPath) || normalizedPath.startsWith("/api/internal/");
  }

  /**
   * True when the request must carry a valid Bearer JWT (all of {@code /api/**} except auth and
   * internal).
   */
  public boolean requiresJwtBearer(String normalizedPath) {
    return normalizedPath.startsWith("/api/") && !isJwtSkipped(normalizedPath);
  }

  /**
   * True when {@link TenantResolutionFilter} must validate {@code X-Tenant-Id} and membership.
   * Excludes e.g. {@code GET /api/tenants} (list) and {@code POST /api/tenants/create}.
   */
  public boolean requiresTenantHeader(String normalizedPath) {
    if ("/api/tenants/create".equals(normalizedPath)) {
      return false;
    }
    return normalizedPath.startsWith("/api/users") || normalizedPath.startsWith("/api/tenants/");
  }
}
