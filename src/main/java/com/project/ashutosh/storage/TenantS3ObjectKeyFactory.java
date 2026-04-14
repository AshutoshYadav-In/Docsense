package com.project.ashutosh.storage;

import com.project.ashutosh.tenant.TenantContext;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Builds S3 object keys partitioned by tenant. Keys have the form:
 *
 * <pre>
 * tenants/{tenantReferenceId}/{relativePath}
 * </pre>
 *
 * where {@code tenantReferenceId} is the external tenant id (same as {@code X-Tenant-Id} and {@link
 * TenantContext.Context#referenceId()}), not the internal numeric database id. Use lowercase UUID
 * strings for stable, client-aligned paths. Environment separation is via the bucket name in
 * configuration, not a prefix in the key.
 */
@Component
public final class TenantS3ObjectKeyFactory {

  private static final String TENANTS_ROOT = "tenants";

  /**
   * Prefix for all objects belonging to a tenant, ending with {@code /}.
   *
   * @param tenantReferenceId external tenant id (must not be null)
   * @return e.g. {@code tenants/550e8400-e29b-41d4-a716-446655440000/}
   */
  public String prefixForTenant(UUID tenantReferenceId) {
    Objects.requireNonNull(tenantReferenceId, "tenantReferenceId");
    return TENANTS_ROOT + "/" + tenantReferenceId.toString().toLowerCase() + "/";
  }

  /**
   * Full object key under the tenant root. Each argument is one path segment (no {@code /} inside a
   * segment).
   *
   * @param tenantReferenceId external tenant id
   * @param relativeSegments one or more safe segments (e.g. file name or nested folders)
   * @return full S3 object key
   * @throws IllegalArgumentException if segments are missing, null, empty, {@code .}, {@code ..}, or
   *     contain separators or control characters
   */
  public String objectKey(UUID tenantReferenceId, String... relativeSegments) {
    if (relativeSegments == null || relativeSegments.length == 0) {
      throw new IllegalArgumentException("relative path must have at least one segment");
    }
    String relative = joinNormalizedSegments(relativeSegments);
    return prefixForTenant(tenantReferenceId) + relative;
  }

  private static String joinNormalizedSegments(String... segments) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < segments.length; i++) {
      String segment = segments[i];
      if (segment == null) {
        throw new IllegalArgumentException("path segment must not be null");
      }
      String validated = validateSegment(segment);
      if (i > 0) {
        sb.append('/');
      }
      sb.append(validated);
    }
    return sb.toString();
  }

  private static String validateSegment(String segment) {
    String trimmed = segment.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("path segment must not be empty");
    }
    if (".".equals(trimmed) || "..".equals(trimmed)) {
      throw new IllegalArgumentException("invalid path segment: " + trimmed);
    }
    if (trimmed.indexOf('/') >= 0 || trimmed.indexOf('\\') >= 0) {
      throw new IllegalArgumentException("path segment must not contain path separators");
    }
    for (int i = 0; i < trimmed.length(); i++) {
      char c = trimmed.charAt(i);
      if (c < 32) {
        throw new IllegalArgumentException("path segment contains control characters");
      }
    }
    return trimmed;
  }
}
