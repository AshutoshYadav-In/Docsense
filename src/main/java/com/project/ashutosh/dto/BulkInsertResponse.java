package com.project.ashutosh.dto;

import java.util.UUID;

public record BulkInsertResponse(
    String status, UUID referenceId, int indexedCount, String indexName) {}
