package com.project.ashutosh.dto;

import java.util.List;

/**
 * Debug-oriented vector search result; later this can wrap LLM output instead of raw hits.
 *
 * @param indexName OpenSearch index searched (for debugging)
 */
public record VectorSearchResponse(String query, String indexName, List<ChunkSearchHit> hits) {}
