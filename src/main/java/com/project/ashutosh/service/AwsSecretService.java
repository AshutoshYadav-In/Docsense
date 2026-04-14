package com.project.ashutosh.service;

import ch.qos.logback.core.util.StringUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ashutosh.model.ApplicationSecret;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

@Service
public class AwsSecretService {

  @Value("${aws.region}")
  public String AWS_REGION;

  @Value("${aws.profile:}")
  public String AWS_PROFILE;

  private final ObjectMapper objectMapper;
  private SecretsManagerClient secretsManagerClient;

  public AwsSecretService(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @PostConstruct
  void initSecretsManagerClient() {
    AwsCredentialsProvider awsCredentialsProvider = credentialsProviderFor(AWS_PROFILE);
    this.secretsManagerClient =
        SecretsManagerClient.builder()
            .credentialsProvider(awsCredentialsProvider)
            .region(Region.of(AWS_REGION))
            .build();
  }

  private static AwsCredentialsProvider credentialsProviderFor(String awsProfile) {
    if (!StringUtil.isNullOrEmpty(awsProfile)) {
      return ProfileCredentialsProvider.builder().profileName(awsProfile).build();
    }
    return DefaultCredentialsProvider.builder().build();
  }

  public String getSecretValue(String secretId) {
    GetSecretValueRequest request = GetSecretValueRequest.builder().secretId(secretId).build();
    GetSecretValueResponse secretValue = secretsManagerClient.getSecretValue(request);
    String str = secretValue.secretString();
    if (StringUtil.isNullOrEmpty(str)) {
      throw new IllegalStateException();
    }
    return str;
  }

  public ApplicationSecret getApplicationSecret(String secretName) {
    try {
      return objectMapper.readValue(getSecretValue(secretName), ApplicationSecret.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(e);
    }
  }
}
