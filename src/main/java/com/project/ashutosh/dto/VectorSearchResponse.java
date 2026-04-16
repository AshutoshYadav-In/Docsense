package com.project.ashutosh.dto;

import java.util.List;

/**
 * Public search response: query, AI answer, and document names the AI actually used.
 *
 * @param sources document names cited by the AI (from its structured JSON output, not server-side guess)
 */
public record VectorSearchResponse(String query, String answer, List<String> sources) {}
