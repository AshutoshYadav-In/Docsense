package com.project.ashutosh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ChunkWithEmbedding(@NotBlank String text, @NotNull List<Float> embedding) {}
