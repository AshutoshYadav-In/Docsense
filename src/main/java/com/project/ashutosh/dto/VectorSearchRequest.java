package com.project.ashutosh.dto;

import jakarta.validation.constraints.NotBlank;

public record VectorSearchRequest(@NotBlank String query) {}
