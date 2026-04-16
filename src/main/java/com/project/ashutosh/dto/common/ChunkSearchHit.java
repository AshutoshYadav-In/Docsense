package com.project.ashutosh.dto.common;

/**
 * One chunk matched by vector search. {@code relevanceScore} is OpenSearch k-NN cosine similarity
 * (higher is better; filtered by configured minimum).
 */
public record ChunkSearchHit(
    String text,
    double relevanceScore,
    String referenceId,
    String fileName,
    int chunkIndex) {}
