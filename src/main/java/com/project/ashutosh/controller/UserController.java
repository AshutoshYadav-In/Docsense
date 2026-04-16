package com.project.ashutosh.controller;

import com.project.ashutosh.dto.CreateUserRequest;
import com.project.ashutosh.dto.TenantContextResponse;
import com.project.ashutosh.dto.UserResponse;
import com.project.ashutosh.service.TenantRequestContextService;
import com.project.ashutosh.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tenant-scoped routes require {@code Authorization: Bearer} and {@code X-Tenant-Id} with the
 * tenant's external {@code referenceId} (UUID in {@code X-Tenant-Id}). Membership is validated per
 * request in {@link com.project.ashutosh.security.TenantResolutionFilter}.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

  @Autowired
  private UserService userService;

  @Autowired
  private TenantRequestContextService tenantRequestContextService;

  @GetMapping("/user/tenant-context")
  public TenantContextResponse tenantContext() {
    return tenantRequestContextService.requireTenantContextResponse();
  }

  @GetMapping("/{id}")
  public UserResponse get(@PathVariable Long id) {
    return userService.getUser(id);
  }

  @PostMapping
  public ResponseEntity<UserResponse> create(@RequestBody CreateUserRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    userService.deleteUser(id);
  }
}
