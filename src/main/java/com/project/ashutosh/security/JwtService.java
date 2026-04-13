package com.project.ashutosh.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final SecretKey signingKey;
  private final long expirationMs;

  public JwtService(
      @Value("${jwt.secret}") String secret,
      @Value("${jwt.expiration-ms}") long expirationMs) {
    if (secret.length() < 32) {
      throw new IllegalStateException("jwt.secret must be at least 32 characters (256 bits) for HS256");
    }
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.expirationMs = expirationMs;
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
