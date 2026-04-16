package com.project.ashutosh.security;

import com.project.ashutosh.secret.ApplicationSecret;
import com.project.ashutosh.secret.JwtSecret;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Getter
@Setter
public class JwtService {

  @Autowired
  private ApplicationSecret applicationSecret;

  private SecretKey signingKey;
  private long expirationMs;

  @PostConstruct
  void initFromApplicationSecret() {
    JwtSecret jwt = jwtSecretFromApplicationSecret();
    String secret = jwt.getSecret();
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException(
          "ApplicationSecret.jwtSecret.secret must be set and at least 32 characters for HS256");
    }
    Long exp = jwt.getExpiration();
    if (exp == null || exp <= 0) {
      throw new IllegalStateException("ApplicationSecret.jwtSecret.expiration must be positive");
    }
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = exp;
  }

  /** JWT settings from the autowired {@link ApplicationSecret} (AWS Secrets Manager JSON). */
  private JwtSecret jwtSecretFromApplicationSecret() {
    JwtSecret jwt = applicationSecret.getJwtSecret();
    if (jwt == null) {
      throw new IllegalStateException("ApplicationSecret.jwtSecret is required");
    }
    return jwt;
  }

  public String generateToken(Long userId, String email) {
    Instant now = Instant.now();
    Instant exp = now.plusMillis(expirationMs);
    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("email", email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(signingKey)
        .compact();
  }

  public Claims parseValidToken(String token) throws JwtException {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  public boolean isTokenValid(String token) {
    try {
      parseValidToken(token);
      return true;
    } catch (JwtException e) {
      return false;
    }
  }
}
