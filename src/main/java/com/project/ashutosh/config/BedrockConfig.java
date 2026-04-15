package com.project.ashutosh.config;

import ch.qos.logback.core.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

/**
 * Bedrock Runtime for RAG answers (same region/profile pattern as {@link S3Config}). Model id is in
 * {@code application.properties}.
 */
@Configuration
public class BedrockConfig {

  @Value("${aws.region}")
  public String awsRegion;

  @Value("${aws.profile:}")
  public String awsProfile;

  @Bean
  public BedrockRuntimeClient bedrockRuntimeClient() {
    AwsCredentialsProvider credentialsProvider = credentialsProviderFor(awsProfile);
    return BedrockRuntimeClient.builder().credentialsProvider(credentialsProvider).region(Region.of(awsRegion)).build();
  }

  private static AwsCredentialsProvider credentialsProviderFor(String awsProfile) {
    if (!StringUtil.isNullOrEmpty(awsProfile)) {
      return ProfileCredentialsProvider.builder().profileName(awsProfile).build();
    }
    return DefaultCredentialsProvider.builder().build();
  }
}
