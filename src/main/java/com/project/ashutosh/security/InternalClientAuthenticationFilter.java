package com.project.ashutosh.security;

import ch.qos.logback.core.util.StringUtil;
import com.project.ashutosh.secret.ApplicationSecret;
import com.project.ashutosh.secret.ClientSecret;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates {@code /api/internal/**} using {@code X-Client-Id} and {@code X-Client-Token}
 * matched against {@link ApplicationSecret#getClientSecret()}. JWT is not used for these routes.
 */
public class InternalClientAuthenticationFilter extends OncePerRequestFilter {

  public static final String CLIENT_ID_HEADER = "X-Client-Id";
  public static final String CLIENT_TOKEN_HEADER = "X-Client-Token";

  private final ApplicationSecret applicationSecret;
  private final ApiPathPatterns apiPathPatterns;

  public InternalClientAuthenticationFilter(
      ApplicationSecret applicationSecret, ApiPathPatterns apiPathPatterns) {
    this.applicationSecret = applicationSecret;
    this.apiPathPatterns = apiPathPatterns;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = apiPathPatterns.normalizedPath(request);
    if (!path.startsWith("/api/internal")) {
      filterChain.doFilter(request, response);
      return;
    }

    ClientSecret expected = applicationSecret.getClientSecret();
    if (expected == null
        || StringUtil.isNullOrEmpty(expected.getClientId())
        || StringUtil.isNullOrEmpty(expected.getClientToken())) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Internal client not configured");
      return;
    }

    String clientId = request.getHeader(CLIENT_ID_HEADER);
    String clientToken = request.getHeader(CLIENT_TOKEN_HEADER);
    if (StringUtil.isNullOrEmpty(clientId) || StringUtil.isNullOrEmpty(clientToken)) {
      response.sendError(
          HttpServletResponse.SC_UNAUTHORIZED,
          "Missing " + CLIENT_ID_HEADER + " or " + CLIENT_TOKEN_HEADER);
      return;
    }

    if (!constantTimeEquals(clientId, expected.getClientId())
        || !constantTimeEquals(clientToken, expected.getClientToken())) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid client credentials");
      return;
    }

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(clientId, null, Collections.emptyList());
    auth.setDetails(Collections.singletonMap(HttpHeaders.AUTHORIZATION, "internal"));
    SecurityContextHolder.getContext().setAuthentication(auth);
    filterChain.doFilter(request, response);
  }

  private static boolean constantTimeEquals(String a, String b) {
    byte[] left = a.getBytes(StandardCharsets.UTF_8);
    byte[] right = b.getBytes(StandardCharsets.UTF_8);
    if (left.length != right.length) {
      return false;
    }
    return MessageDigest.isEqual(left, right);
  }
}
