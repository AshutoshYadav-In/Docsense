package com.project.ashutosh.dto.response;

import java.util.UUID;

/** Returned after a tenant uploads a document; matches fields sent to Step Functions for ingestion. */
public record DocumentUploadResponse(
    UUID referenceId,
    String folderName,
    String fileName,
    String bucket,
    String s3Key,
    String s3FolderPrefix) {}
