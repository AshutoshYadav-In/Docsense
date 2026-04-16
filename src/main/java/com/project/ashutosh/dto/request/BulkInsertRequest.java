package com.project.ashutosh.dto.request;

import com.project.ashutosh.dto.common.ChunkWithEmbedding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record BulkInsertRequest(
    @NotNull UUID referenceId,
    @NotBlank String fileName,
    @NotEmpty @Valid List<ChunkWithEmbedding> chunksWithEmbeddings) {}
