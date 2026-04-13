package com.project.ashutosh.controller;

import com.project.ashutosh.dto.CreateTenantRequest;
import com.project.ashutosh.dto.OnboardTenantMembersRequest;
import com.project.ashutosh.dto.OnboardTenantMembersResponse;
import com.project.ashutosh.dto.TenantResponse;
import com.project.ashutosh.service.TenantService;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("isAuthenticated()")
public class TenantController {

  private final TenantService tenantService;

  public TenantController(TenantService tenantService) {
    this.tenantService = tenantService;
  }

  @PostMapping
  public ResponseEntity<TenantResponse> createTenant(@RequestBody CreateTenantRequest request) {
    try {
      TenantResponse created =
          tenantService.createTenant(currentUserId(), request);
      return ResponseEntity.status(HttpStatus.CREATED).body(created);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  @GetMapping
  public List<TenantResponse> listMyTenants() {
    return tenantService.listTenantsForUser(currentUserId());
  }

  @PostMapping("/members")
  public ResponseEntity<OnboardTenantMembersResponse> onboardMembers(
      @Valid @RequestBody OnboardTenantMembersRequest request) {
    try {
      OnboardTenantMembersResponse body = tenantService.onboardEmails(request.getEmails());
      return ResponseEntity.ok(body);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }

  private static Long currentUserId() {
    return Long.parseLong(
        SecurityContextHolder.getContext().getAuthentication().getName());
  }
}
