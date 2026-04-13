package com.project.ashutosh.controller;

import com.project.ashutosh.dto.CreateUserRequest;
import com.project.ashutosh.dto.TenantContextResponse;
import com.project.ashutosh.dto.UserResponse;
import com.project.ashutosh.service.UserService;
import com.project.ashutosh.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant-scoped routes require {@code Authorization: Bearer} and {@code X-Tenant-Id} with the
 * tenant's external {@code reference_id} (UUID). Membership is validated per request.
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("isAuthenticated()")
public class UserController {

  private final UserService userService;
  private final TenantContext tenantContext;

  public UserController(UserService userService, TenantContext tenantContext) {
    this.userService = userService;
    this.tenantContext = tenantContext;
  }

  /** Confirms tenant resolution: internal id, reference id, and name for the current request. */
  @GetMapping("/user/tenant-context")
  public ResponseEntity<TenantContextResponse> tenantContext() {
    return tenantContext
        .get()
        .map(ctx -> ResponseEntity.ok(TenantContextResponse.fromContext(ctx)))
        .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserResponse> get(@PathVariable Long id) {
    return userService.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
  }

  @PostMapping
  public ResponseEntity<UserResponse> create(@RequestBody CreateUserRequest request) {
    try {
      UserResponse created = userService.createUser(request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    if (userService.findById(id).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    userService.deleteById(id);
    return ResponseEntity.noContent().build();
  }
}
