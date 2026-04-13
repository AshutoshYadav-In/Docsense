package com.project.ashutosh.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * MVC hook for {@code /api/users/**}: logs authenticated access without re-parsing JWT (auth is
 * established by {@link com.project.ashutosh.security.JwtAuthenticationFilter}).
 */
@Component
public class UserApiLoggingInterceptor implements HandlerInterceptor {

  private static final Logger log = LoggerFactory.getLogger(UserApiLoggingInterceptor.class);

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (!log.isDebugEnabled()) {
      return true;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      log.debug(
          "userApi {} {} principal={}",
          request.getMethod(),
          request.getRequestURI(),
          auth.getName());
    }
    return true;
  }
}
