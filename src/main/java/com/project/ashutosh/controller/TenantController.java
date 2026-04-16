package com.project.ashutosh.controller;

import com.project.ashutosh.dto.CreateTenantRequest;
import com.project.ashutosh.dto.OnboardTenantMembersRequest;
import com.project.ashutosh.dto.OnboardTenantMembersResponse;
import com.project.ashutosh.dto.TenantResponse;
import com.project.ashutosh.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("isAuthenticated()")
public class TenantController {

  @Autowired
  private TenantService tenantService;

  @PostMapping("/create")
  public ResponseEntity<TenantResponse> createTenant(@RequestBody CreateTenantRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(tenantService.createTenant(request));
  }

  @GetMapping
  public List<TenantResponse> listMyTenants() {
    return tenantService.listTenantsForCurrentUser();
  }

  @PostMapping("/members")
  public ResponseEntity<OnboardTenantMembersResponse> onboardMembers(
      @Valid @RequestBody OnboardTenantMembersRequest request) {
    return ResponseEntity.ok(tenantService.onboardEmails(request.getEmails()));
  }
}
