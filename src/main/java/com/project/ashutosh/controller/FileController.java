package com.project.ashutosh.controller;

import com.project.ashutosh.dto.DocumentUploadResponse;
import com.project.ashutosh.service.TenantDocumentUploadService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tenants/files")
@PreAuthorize("isAuthenticated()")
public class FileController {

  private final TenantDocumentUploadService tenantDocumentUploadService;

  public FileController(TenantDocumentUploadService tenantDocumentUploadService) {
    this.tenantDocumentUploadService = tenantDocumentUploadService;
  }

  @PostMapping
  public DocumentUploadResponse upload(@RequestParam("file") MultipartFile file) {
    return tenantDocumentUploadService.upload(file);
  }
}
