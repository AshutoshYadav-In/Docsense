package com.project.ashutosh.service;

import ch.qos.logback.core.util.StringUtil;
import com.project.ashutosh.dao.DocumentJobDao;
import com.project.ashutosh.dto.event.DocumentIngestionEvent;
import com.project.ashutosh.dto.response.DocumentUploadResponse;
import com.project.ashutosh.entity.DocumentJob;
import com.project.ashutosh.enums.DocumentJobStatus;
import com.project.ashutosh.security.CurrentUserIdProvider;
import com.project.ashutosh.storage.TenantS3ObjectKeyFactory;
import com.project.ashutosh.tenant.TenantContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Transactional
public class TenantDocumentUploadService {

  private static final int MAX_FILENAME_LENGTH = 200;

  @Value("${aws.s3.bucket}")
  public String AWS_S3_BUCKET;

  private final TenantContext tenantContext;
  private final TenantS3ObjectKeyFactory s3ObjectKeyFactory;
  private final S3Client s3Client;
  private final DocumentJobDao documentJobDao;
  private final CurrentUserIdProvider currentUserIdProvider;
  private final StepFunctionIngestionService stepFunctionIngestionService;

  public TenantDocumentUploadService(
      TenantContext tenantContext,
      TenantS3ObjectKeyFactory s3ObjectKeyFactory,
      S3Client s3Client,
      DocumentJobDao documentJobDao,
      CurrentUserIdProvider currentUserIdProvider,
      StepFunctionIngestionService stepFunctionIngestionService) {
    this.tenantContext = tenantContext;
    this.s3ObjectKeyFactory = s3ObjectKeyFactory;
    this.s3Client = s3Client;
    this.documentJobDao = documentJobDao;
    this.currentUserIdProvider = currentUserIdProvider;
    this.stepFunctionIngestionService = stepFunctionIngestionService;
  }

  public DocumentUploadResponse upload(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
    }
    if (StringUtil.isNullOrEmpty(AWS_S3_BUCKET)) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "S3 bucket not configured");
    }
    UUID tenantReferenceId = tenantContext.get().map(TenantContext.Context::referenceId).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "Tenant context missing after tenant filter"));

    UUID jobReferenceId = UUID.randomUUID();
    documentJobDao.save(
        DocumentJob.builder().referenceId(jobReferenceId).creatorId(currentUserIdProvider.requireUserId())
            .status(DocumentJobStatus.PROCESSING).build());

    String folderName = jobReferenceId.toString();

    String key = s3ObjectKeyFactory.objectKey(tenantReferenceId, folderName, file.getOriginalFilename());
    String folderPrefix = s3ObjectKeyFactory.documentFolderPrefix(tenantReferenceId, folderName);

    String contentType = file.getContentType();
    PutObjectRequest.Builder putBuilder =
        PutObjectRequest.builder().bucket(AWS_S3_BUCKET).key(key);
    if (contentType != null && !contentType.isBlank()) {
      putBuilder.contentType(contentType);
    }
    PutObjectRequest putRequest = putBuilder.build();

    try (InputStream in = file.getInputStream()) {
      s3Client.putObject(putRequest, RequestBody.fromInputStream(in, file.getSize()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    DocumentIngestionEvent event = new DocumentIngestionEvent(key, folderPrefix, folderName, file.getOriginalFilename(),
        jobReferenceId,
        file.getOriginalFilename(), contentType, file.getSize(), AWS_S3_BUCKET);
    stepFunctionIngestionService.publish(event);
    return new DocumentUploadResponse(
        jobReferenceId, folderName, file.getOriginalFilename(), AWS_S3_BUCKET, key, folderPrefix);
  }
}
