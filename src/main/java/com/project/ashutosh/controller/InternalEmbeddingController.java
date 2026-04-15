package com.project.ashutosh.controller;

import com.project.ashutosh.dto.BulkInsertRequest;
import com.project.ashutosh.dto.BulkInsertResponse;
import com.project.ashutosh.dto.EmbedBatchRequest;
import com.project.ashutosh.dto.EmbedBatchResponse;
import com.project.ashutosh.service.InternalEmbeddingService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
public class InternalEmbeddingController {

  @Autowired
  private InternalEmbeddingService internalEmbeddingService;

  @PostMapping("/embed")
  public EmbedBatchResponse embed(@Valid @RequestBody EmbedBatchRequest request) throws Exception {
    return internalEmbeddingService.embedBatch(request);
  }

  @PostMapping("/bulk-insert")
  public BulkInsertResponse bulkInsert(@Valid @RequestBody BulkInsertRequest request) {
    return internalEmbeddingService.bulkInsert(request);
  }
}
