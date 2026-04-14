package com.project.ashutosh.config;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

  /**
   * Global JSON naming: snake_case in JSON, camelCase in Java (REST API and AWS Secrets Manager
   * secret string must use snake_case keys, e.g. {@code jwt_secret}, {@code database_credentials}).
   */
  @Bean
  public Jackson2ObjectMapperBuilderCustomizer snakeCaseJsonCustomizer() {
    return builder -> builder.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
  }
}
