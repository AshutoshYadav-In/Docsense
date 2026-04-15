package com.project.ashutosh.service;

import com.project.ashutosh.dto.ChunkSearchHit;
import com.project.ashutosh.dto.VectorSearchResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ChunkVectorSearchService {

  private static final String ES_FIELD_REFERENCE_ID = "reference_id";
  private static final String ES_FIELD_FILE_NAME = "file_name";
  private static final String ES_FIELD_CHUNK_INDEX = "chunk_index";
  private static final String ES_FIELD_TEXT = "text";
  private static final String ES_FIELD_EMBEDDING = "embedding";

  private final EmbeddingService embeddingService;
  private final OpenSearchClient openSearchClient;
  private final RagBedrockService ragBedrockService;

  @Value("${opensearch.index-name}")
  private String openSearchIndexName;

  @Value("${opensearch.search.top-k}")
  private int topK;

  /** Cosine-similarity floor (0–1); hits below this are excluded. */
  @Value("${opensearch.search.min-relevance-score}")
  private float minRelevanceScore;

  /** Neighbors considered before applying {@link #minRelevanceScore} and taking top {@link #topK}. */
  @Value("${opensearch.search.knn-neighbors}")
  private int knnNeighbors;

  public ChunkVectorSearchService(
      EmbeddingService embeddingService,
      OpenSearchClient openSearchClient,
      RagBedrockService ragBedrockService) {
    this.embeddingService = embeddingService;
    this.openSearchClient = openSearchClient;
    this.ragBedrockService = ragBedrockService;
  }

  public VectorSearchResponse search(String queryText) throws Exception {
    if (queryText == null || queryText.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query is required");
    }
    String trimmed = queryText.trim();
    float[] vector = embeddingService.embed(trimmed);

    int neighbors = Math.max(knnNeighbors, topK);

    // kNN: set only `k` on the query — OpenSearch rejects `k` + `minScore` together ("exactly one of k,
    // distance or score"). Apply the relevance floor in Java below.
    SearchRequest request = SearchRequest.of(s -> s.index(openSearchIndexName).size(neighbors)
        .query(Query.of(q -> q.knn(k -> k.field(ES_FIELD_EMBEDDING).vector(vector).k(neighbors)))));

    try {
      SearchResponse<JsonData> response = openSearchClient.search(request, JsonData.class);
      List<ChunkSearchHit> candidates = new ArrayList<>();
      for (Hit<JsonData> hit : response.hits().hits()) {
        JsonData source = hit.source();
        if (source == null) {
          continue;
        }
        Map<String, Object> src = source.to(Map.class);
        String text = stringField(src, ES_FIELD_TEXT);
        String refId = stringField(src, ES_FIELD_REFERENCE_ID);
        String fileName = stringField(src, ES_FIELD_FILE_NAME);
        int chunkIndex = intField(src, ES_FIELD_CHUNK_INDEX);
        double score = hit.score() != null ? hit.score() : 0.0;
        candidates.add(new ChunkSearchHit(text, score, refId, fileName, chunkIndex));
      }
      candidates.sort(Comparator.comparingDouble(ChunkSearchHit::relevanceScore).reversed());
      List<ChunkSearchHit> hits = new ArrayList<>(topK);
      for (ChunkSearchHit h : candidates) {
        if (h.relevanceScore() < minRelevanceScore) {
          continue;
        }
        hits.add(h);
        if (hits.size() >= topK) {
          break;
        }
      }
      String answer = ragBedrockService.answerFromChunks(trimmed, hits);
      return new VectorSearchResponse(trimmed, openSearchIndexName, hits, answer);
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "OpenSearch search failed", e);
    }
  }

  private static String stringField(Map<String, Object> src, String key) {
    Object v = src.get(key);
    return v == null ? "" : String.valueOf(v);
  }

  private static int intField(Map<String, Object> src, String key) {
    Object v = src.get(key);
    if (v instanceof Number n) {
      return n.intValue();
    }
    return -1;
  }
}
