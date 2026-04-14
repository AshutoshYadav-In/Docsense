package com.project.ashutosh.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationSecret {

  private JwtSecret jwtSecret;
  private DatabaseCredentials databaseCredentials;
  private ClientSecret clientSecret;
  private ElasticsearchCredentials elasticsearchCredentials;
}
