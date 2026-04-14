package com.project.ashutosh.controller;

import com.project.ashutosh.dto.EmbedRequest;
import com.project.ashutosh.dto.EmbedResponse;
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
  public EmbedResponse embed(@Valid @RequestBody EmbedRequest request) throws Exception {
    return internalEmbeddingService.embed(request);
  }
}
