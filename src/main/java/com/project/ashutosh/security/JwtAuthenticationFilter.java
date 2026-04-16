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
 * Enforces Bearer JWT on all {@code /api/**} routes except those skipped by {@link ApiPathPatterns}
 * ({@code /api/auth/**}, {@code /api/internal/**}). Other paths may still send an optional Bearer
 * token (invalid token yields 401). {@code /api/internal/**} is skipped entirely (see {@link
 * InternalClientAuthenticationFilter}).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final ApiPathPatterns apiPathPatterns;

  public JwtAuthenticationFilter(JwtService jwtService, ApiPathPatterns apiPathPatterns) {
    this.jwtService = jwtService;
    this.apiPathPatterns = apiPathPatterns;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = apiPathPatterns.normalizedPath(request);
    if (apiPathPatterns.isJwtSkipped(path)) {
      filterChain.doFilter(request, response);
      return;
    }
    if (apiPathPatterns.requiresJwtBearer(path)) {
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

  /**
   * Parses {@code Authorization: Bearer <token>}, validates with {@link JwtService}, and sets the
   * security context. When {@code required} is true, missing or invalid credentials yield 401 and
   * {@code false}.
   */
  private boolean authenticateFromBearerHeader(
      HttpServletRequest request, HttpServletResponse response, boolean required)
      throws IOException {
    String header = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (StringUtil.isNullOrEmpty(header)) {
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
