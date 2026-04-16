package com.project.ashutosh.secret;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ElasticsearchCredentials {

  /**
   * OpenSearch domain endpoint URL (e.g. {@code https://search-xxx.us-east-1.es.amazonaws.com}).
   * Used to derive the host; requests are signed with IAM (no master user/password).
   */
  private String uri;
}
