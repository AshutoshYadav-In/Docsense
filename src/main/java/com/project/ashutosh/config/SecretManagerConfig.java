package com.project.ashutosh.config;

import com.project.ashutosh.model.ApplicationSecret;
import com.project.ashutosh.service.AwsSecretService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecretManagerConfig {

  @Value("${aws.secret.name}")
  private String AWS_SECRET_NAME;

  @Bean
  public ApplicationSecret applicationSecret(AwsSecretService awsSecretService) {
    return awsSecretService.getApplicationSecret(AWS_SECRET_NAME);
  }
}
