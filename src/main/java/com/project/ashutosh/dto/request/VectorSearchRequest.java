package com.project.ashutosh.dto.request;

import jakarta.validation.constraints.NotBlank;

public record VectorSearchRequest(@NotBlank String query) {}
