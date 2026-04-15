package com.project.ashutosh.dto;

/** Public search API: user query and RAG answer only (retrieval details stay server-side). */
public record VectorSearchResponse(String query, String answer) {}
