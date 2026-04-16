package com.project.ashutosh.dto.event;

import java.util.UUID;

/**
 * Payload pushed to Step Functions after storing an uploaded file in S3. The object lives at
 * {@code s3Key} under the tenant; {@code s3FolderPrefix} is the document-scoped prefix (same folder as
 * the file) so the async pipeline can write embeddings and other artifacts as sibling keys.
 */
public record DocumentIngestionEvent(
    String s3Key,
    String s3FolderPrefix,
    String folderName,
    String fileName,
    UUID referenceId,
    String originalFilename,
    String contentType,
    long size,
    String bucket) {}
