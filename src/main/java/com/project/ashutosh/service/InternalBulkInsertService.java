package com.project.ashutosh.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.json.JsonData;
import com.project.ashutosh.dao.DocumentJobDao;
import com.project.ashutosh.dto.BulkInsertRequest;
import com.project.ashutosh.dto.BulkInsertResponse;
import com.project.ashutosh.dto.ChunkWithEmbedding;
import com.project.ashutosh.entity.DocumentJobStatus;
import com.project.ashutosh.model.ApplicationSecret;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InternalBulkInsertService {

  private static final String RESPONSE_STATUS_COMPLETED = "completed";

  /** Elasticsearch document field names (must match your index mapping). */
  private static final String ES_FIELD_REFERENCE_ID = "reference_id";

  private static final String ES_FIELD_FILE_NAME = "file_name";
  private static final String ES_FIELD_CHUNK_INDEX = "chunk_index";
  private static final String ES_FIELD_TEXT = "text";
  private static final String ES_FIELD_EMBEDDING = "embedding";

  private final ElasticsearchClient elasticsearchClient;
  private final ApplicationSecret applicationSecret;
  private final DocumentJobDao documentJobDao;
  private final int embeddingDimensions;

  public InternalBulkInsertService(
      ElasticsearchClient elasticsearchClient,
      ApplicationSecret applicationSecret,
      DocumentJobDao documentJobDao,
      @Value("${embedding.vector.dimensions:384}") int embeddingDimensions) {
    this.elasticsearchClient = elasticsearchClient;
    this.applicationSecret = applicationSecret;
    this.documentJobDao = documentJobDao;
    this.embeddingDimensions = embeddingDimensions;
  }

  @Transactional
  public BulkInsertResponse bulkInsert(BulkInsertRequest request) {
    UUID referenceId = request.referenceId();
    if (!documentJobDao.existsByReferenceId(referenceId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "document job not found");
    }

    List<ChunkWithEmbedding> chunks = request.chunksWithEmbeddings();
    validateEmbeddingDimensions(chunks);

    String indexName = applicationSecret.getElasticsearchCredentials().getIndexName();

    try {
      bulkIndexChunks(indexName, referenceId, request.fileName(), chunks);
    } catch (IOException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_GATEWAY, "Elasticsearch indexing failed", e);
    }

    int updated = documentJobDao.updateStatusByReferenceId(referenceId, DocumentJobStatus.COMPLETED);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "document job not found");
    }

    return new BulkInsertResponse(
        RESPONSE_STATUS_COMPLETED, referenceId, chunks.size(), indexName);
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

  private void bulkIndexChunks(
      String indexName, UUID referenceId, String fileName, List<ChunkWithEmbedding> chunks)
      throws IOException {
    BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
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
      bulkBuilder.operations(
          op -> op.index(idx -> idx.index(indexName).id(docId).document(JsonData.of(doc))));
    }

    BulkResponse response = elasticsearchClient.bulk(bulkBuilder.build());
    if (response.errors()) {
      throw new IOException("Elasticsearch bulk response reported errors");
    }
  }
}
