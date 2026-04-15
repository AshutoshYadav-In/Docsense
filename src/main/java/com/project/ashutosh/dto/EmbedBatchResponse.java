package com.project.ashutosh.dto;

import java.util.List;
import java.util.Map;

/**
 * Embeddings for each id from {@link EmbedBatchRequest#texts()}, same keys as the request.
 */
public record EmbedBatchResponse(Map<String, List<Float>> embeddings) {}
