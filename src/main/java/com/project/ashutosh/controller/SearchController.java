package com.project.ashutosh.controller;

import com.project.ashutosh.dto.request.VectorSearchRequest;
import com.project.ashutosh.dto.response.VectorSearchResponse;
import com.project.ashutosh.service.ChunkVectorSearchService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tenants/search")
public class SearchController {

  @Autowired
  private ChunkVectorSearchService chunkVectorSearchService;

  @PostMapping
  public VectorSearchResponse search(@Valid @RequestBody VectorSearchRequest request) throws Exception {
    return chunkVectorSearchService.search(request.query());
  }
}
