package com.project.ashutosh.controller;

import com.project.ashutosh.service.TenantDocumentUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/tenants/files")
@PreAuthorize("isAuthenticated()")
public class FileController {

  @Autowired
  private TenantDocumentUploadService tenantDocumentUploadService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public void upload(@RequestParam("file") MultipartFile file) {
    tenantDocumentUploadService.upload(file);
  }
}
