package com.project.ashutosh.service;

import com.project.ashutosh.dto.EmbedRequest;
import com.project.ashutosh.dto.EmbedResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class  InternalEmbeddingService {

  private final EmbeddingService embeddingService;

  public InternalEmbeddingService(EmbeddingService embeddingService) {
    this.embeddingService = embeddingService;
  }

  public EmbedResponse embed(EmbedRequest request) throws Exception {
    String chunkText = request.getChunkText();
    float[] vector = embeddingService.embed(chunkText);
    List<Float> embedding = new ArrayList<>(vector.length);
    for (float v : vector) {
      embedding.add(v);
    }
    return EmbedResponse.builder().chunkText(chunkText).embedding(embedding).build();
  }
}
