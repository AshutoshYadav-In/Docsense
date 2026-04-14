package com.project.ashutosh.controller;

import com.project.ashutosh.dto.EmbedRequest;
import com.project.ashutosh.dto.EmbedResponse;
import com.project.ashutosh.service.EmbeddingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
public class InternalEmbeddingController {

  private final EmbeddingService embeddingService;

  public InternalEmbeddingController(EmbeddingService embeddingService) {
    this.embeddingService = embeddingService;
  }

  @PostMapping("/embed")
  public EmbedResponse embed(@Valid @RequestBody EmbedRequest request) throws Exception {
    float[] vector = embeddingService.embed(request.getChunkText());
    return EmbedResponse.of(request.getChunkText(), vector);
  }
}
