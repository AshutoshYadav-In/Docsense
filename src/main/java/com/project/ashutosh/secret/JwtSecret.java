package com.project.ashutosh.secret;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class JwtSecret {

  /** Symmetric key for HS256; at least 32 characters. */
  private String secret;

  /** Access token TTL in milliseconds. */
  private Long expiration;
}
