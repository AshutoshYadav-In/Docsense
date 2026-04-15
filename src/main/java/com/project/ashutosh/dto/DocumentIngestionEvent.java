package com.project.ashutosh.dto;

import java.util.UUID;

/** Payload pushed to Step Functions after storing an uploaded file in S3. */
public record DocumentIngestionEvent(
    String s3Key,
    UUID referenceId,
    String originalFilename,
    String contentType,
    long size,
    String bucket) {}
