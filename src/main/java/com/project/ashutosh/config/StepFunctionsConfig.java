package com.project.ashutosh.config;

import ch.qos.logback.core.util.StringUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sfn.SfnClient;

@Configuration
public class StepFunctionsConfig {

  @Value("${aws.region}")
  public String AWS_REGION;

  @Value("${aws.profile:}")
  public String AWS_PROFILE;

  @Bean
  public SfnClient sfnClient() {
    AwsCredentialsProvider credentialsProvider = credentialsProviderFor(AWS_PROFILE);
    return SfnClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(Region.of(AWS_REGION))
        .build();
  }

  private static AwsCredentialsProvider credentialsProviderFor(String awsProfile) {
    if (!StringUtil.isNullOrEmpty(awsProfile)) {
      return ProfileCredentialsProvider.builder().profileName(awsProfile).build();
    }
    return DefaultCredentialsProvider.builder().build();
  }
}
