package com.project.ashutosh.dto.response;

import java.util.UUID;

public record BulkInsertResponse(
    String status, UUID referenceId, int indexedCount, String indexName) {}
