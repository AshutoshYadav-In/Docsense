package com.project.ashutosh.service;

import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmbeddingService {

  @Value(
      "${djl.embedding.model-url:djl://ai.djl.huggingface.pytorch/sentence-transformers/all-MiniLM-L6-v2}")
  private String modelUrl;

  private ZooModel<String, float[]> model;
  private Predictor<String, float[]> predictor;

  @PostConstruct
  void loadModel() throws Exception {
    Criteria<String, float[]> criteria =
        Criteria.builder()
            .setTypes(String.class, float[].class)
            .optModelUrls(modelUrl)
            .optEngine("PyTorch")
            .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
            .build();
    model = ModelZoo.loadModel(criteria);
    predictor = model.newPredictor();
  }

  @PreDestroy
  void close() {
    if (predictor != null) {
      predictor.close();
    }
    if (model != null) {
      model.close();
    }
  }

  /** Returns embedding vector (e.g. 384 dimensions for all-MiniLM-L6-v2). */
  public synchronized float[] embed(String text) throws Exception {
    return predictor.predict(text);
  }
}
