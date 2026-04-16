package com.project.ashutosh.service;

import com.project.ashutosh.dao.DocumentJobDao;
import com.project.ashutosh.dto.common.ChunkWithEmbedding;
import com.project.ashutosh.dto.request.BulkInsertRequest;
import com.project.ashutosh.dto.request.EmbedBatchRequest;
import com.project.ashutosh.dto.response.BulkInsertResponse;
import com.project.ashutosh.dto.response.EmbedBatchResponse;
import com.project.ashutosh.enums.DocumentJobStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InternalEmbeddingService {

  private static final String RESPONSE_STATUS_COMPLETED = "completed";

  /**
   * OpenSearch field names (must match the index mapping). {@link #ES_FIELD_EMBEDDING} should be
   * {@code knn_vector} (native k-NN on Amazon OpenSearch) with {@code dimension} matching
   * {@code embedding.vector.dimensions}; see {@code opensearch/document-chunks-index-template.json}.
   */
  private static final String ES_FIELD_REFERENCE_ID = "reference_id";

  private static final String ES_FIELD_FILE_NAME = "file_name";
  private static final String ES_FIELD_CHUNK_INDEX = "chunk_index";
  private static final String ES_FIELD_TEXT = "text";

  /** Document key for the float vector; index mapping type {@code knn_vector}. */
  private static final String ES_FIELD_EMBEDDING = "embedding";

  private final EmbeddingService embeddingService;
  private final OpenSearchClient openSearchClient;
  private final DocumentJobDao documentJobDao;

  @Value("${embedding.vector.dimensions}")
  private int embeddingDimensions;
  @Value("${opensearch.index-name}")
  private String openSearchIndexName;

  public InternalEmbeddingService(EmbeddingService embeddingService, OpenSearchClient openSearchClient,
      DocumentJobDao documentJobDao) {
    this.embeddingService = embeddingService;
    this.openSearchClient = openSearchClient;
    this.documentJobDao = documentJobDao;
  }

  /**
   * Embeds each text under its id. Request JSON shape: {@code {"texts":{"1":"a","2":"b"}}}. Keys
   * and values must be non-blank; batch size is validated by {@link EmbedBatchRequest} (max
   * {@link EmbedBatchRequest#MAX_TEXTS_PER_REQUEST} texts).
   */
  public EmbedBatchResponse embedBatch(EmbedBatchRequest request) throws Exception {
    Map<String, String> textsById = request.texts();
    validateEmbedBatch(textsById);
    Map<String, List<Float>> out = new LinkedHashMap<>(textsById.size());
    for (Map.Entry<String, String> e : textsById.entrySet()) {
      float[] vector = embeddingService.embed(e.getValue());
      List<Float> embedding = new ArrayList<>(vector.length);
      for (float v : vector) {
        embedding.add(v);
      }
      out.put(e.getKey(), embedding);
    }
    return new EmbedBatchResponse(out);
  }

  private void validateEmbedBatch(Map<String, String> textsById) {
    if (textsById == null || textsById.isEmpty()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "embed request must include at least one id → text entry");
    }
    for (Map.Entry<String, String> e : textsById.entrySet()) {
      String id = e.getKey();
      String text = e.getValue();
      if (id == null || id.isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "embed request ids must be non-blank");
      }
      if (text == null || text.isBlank()) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "embed request text for id \"" + id + "\" must be non-blank");
      }
    }
  }

  @Transactional
  public BulkInsertResponse bulkInsert(BulkInsertRequest request) {
    UUID referenceId = request.referenceId();
    if (!documentJobDao.existsByReferenceId(referenceId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "document job not found");
    }

    List<ChunkWithEmbedding> chunks = request.chunksWithEmbeddings();
    validateEmbeddingDimensions(chunks);

    try {
      bulkIndexChunks(openSearchIndexName, referenceId, request.fileName(), chunks);
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "OpenSearch indexing failed", e);
    }

    int updated =
        documentJobDao.updateCompletionByReferenceId(
            referenceId, DocumentJobStatus.COMPLETED, chunks.size());
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "document job not found");
    }

    return new BulkInsertResponse(
        RESPONSE_STATUS_COMPLETED, referenceId, chunks.size(), openSearchIndexName);
  }

  private void validateEmbeddingDimensions(List<ChunkWithEmbedding> chunks) {
    for (int i = 0; i < chunks.size(); i++) {
      ChunkWithEmbedding c = chunks.get(i);
      if (c.embedding() == null || c.embedding().size() != embeddingDimensions) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "Chunk "
                + i
                + ": embedding must have length "
                + embeddingDimensions
                + " (embedding.vector.dimensions)");
      }
    }
  }

  private void bulkIndexChunks(String indexName, UUID referenceId, String fileName, List<ChunkWithEmbedding> chunks)
      throws IOException {
    List<BulkOperation> operations = new ArrayList<>(chunks.size());
    for (int i = 0; i < chunks.size(); i++) {
      ChunkWithEmbedding chunk = chunks.get(i);
      Map<String, Object> doc = new LinkedHashMap<>();
      doc.put(ES_FIELD_REFERENCE_ID, referenceId.toString());
      doc.put(ES_FIELD_FILE_NAME, fileName);
      doc.put(ES_FIELD_CHUNK_INDEX, i);
      doc.put(ES_FIELD_TEXT, chunk.text());
      float[] vec = new float[chunk.embedding().size()];
      for (int j = 0; j < chunk.embedding().size(); j++) {
        vec[j] = chunk.embedding().get(j);
      }
      doc.put(ES_FIELD_EMBEDDING, vec);
      String docId = referenceId + "_" + i;
      operations.add(BulkOperation.of(o -> o.index(idx -> idx.index(indexName).id(docId).document(JsonData.of(doc)))));
    }

    BulkRequest bulkRequest = BulkRequest.of(b -> b.operations(operations));
    BulkResponse response = openSearchClient.bulk(bulkRequest);
    if (response.errors()) {
      throw new IOException("OpenSearch bulk response reported errors");
    }
  }
}
