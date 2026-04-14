package com.project.ashutosh.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves the authenticated user id from the security context. JWT and security filters run
 * before controllers; if there is no authentication here, the request should not have reached
 * a protected handler (responds with 401 from the filter chain).
 */
@Component
public class CurrentUserIdProvider {

  public long requireUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    }
    return Long.parseLong(auth.getName());
  }
}
