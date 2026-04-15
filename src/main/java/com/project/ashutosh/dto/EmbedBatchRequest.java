package com.project.ashutosh.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

/**
 * Batch text-to-embed payload. Keys are client-defined ids (e.g. {@code "1"}, {@code "2"}); values
 * are the strings to embed.
 */
public record EmbedBatchRequest(
    @NotNull @Size(min = 1, max = EmbedBatchRequest.MAX_TEXTS_PER_REQUEST) Map<String, String> texts) {

  public static final int MAX_TEXTS_PER_REQUEST = 10;
}
