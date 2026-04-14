package com.project.ashutosh.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticsearchCredentials {

  /** One or more URIs (comma-separated). */
  private String uris;

  /** Elastic Cloud encoded API key (Authorization: ApiKey …). */
  private String apiKey;

  private String username;
  private String password;

  private String indexName;
}
