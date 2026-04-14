package com.project.ashutosh.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import ch.qos.logback.core.util.StringUtil;
import java.util.Collections;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces Bearer JWT on {@code /api/users/**} and {@code /api/tenants/**}. Missing or invalid
 * tokens yield {@code 401 Unauthorized} before the controller runs.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (requiresBearerAuth(request)) {
      if (!authenticateFromBearerHeader(request, response, true)) {
        return;
      }
      filterChain.doFilter(request, response);
      return;
    }
    if (!authenticateFromBearerHeader(request, response, false)) {
      return;
    }
    filterChain.doFilter(request, response);
  }

  /** {@code /api/users} and {@code /api/tenants} require a valid Bearer JWT before the controller runs. */
  private static boolean requiresBearerAuth(HttpServletRequest request) {
    String path = TenantResolutionFilter.normalizedPath(request);
    return path.startsWith("/api/users") || path.startsWith("/api/tenants");
  }

  /**
   * Parses {@code Authorization: Bearer <token>}, validates with {@link JwtService}, and sets the
   * security context. When {@code required} is true (user API), missing or invalid credentials
   * yield 401 and {@code false}.
   */
  private boolean authenticateFromBearerHeader(
      HttpServletRequest request, HttpServletResponse response, boolean required)
      throws IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtil.isNullOrEmpty(header) || !header.startsWith("Bearer ")) {
      if (required) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
      }
      return !required;
    }
    String token = header.substring(7);
    if (StringUtil.isNullOrEmpty(token)) {
      if (required) {
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
      }
      return !required;
    }
    try {
      Claims claims = jwtService.parseValidToken(token);
      String userId = claims.getSubject();
      UsernamePasswordAuthenticationToken auth =
          new UsernamePasswordAuthenticationToken(
              userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
      auth.setDetails(claims);
      SecurityContextHolder.getContext().setAuthentication(auth);
      return true;
    } catch (JwtException e) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
      return false;
    }
  }
}
