package com.project.ashutosh.dto;

import java.util.UUID;

/** Response after storing an uploaded file in S3 under the tenant key prefix. */
public record DocumentUploadResponse(
    String s3Key,
    UUID referenceId,
    String originalFilename,
    String contentType,
    long size,
    String bucket) {}
