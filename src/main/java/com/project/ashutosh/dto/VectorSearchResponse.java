package com.project.ashutosh.dto;

import java.util.List;

/**
 * Vector search hits plus a short Bedrock answer built from those passages (RAG).
 *
 * @param indexName OpenSearch index searched (for debugging)
 * @param answer Model reply (3–4 lines); grounded in {@code hits}
 */
public record VectorSearchResponse(String query, String indexName, List<ChunkSearchHit> hits, String answer) {}
