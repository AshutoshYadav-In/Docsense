package com.project.ashutosh.config;

import ch.qos.logback.core.util.StringUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.project.ashutosh.model.ApplicationSecret;
import com.project.ashutosh.model.ElasticsearchCredentials;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

  @Bean
  public RestClient elasticsearchRestClient(ApplicationSecret applicationSecret) {
    ElasticsearchCredentials creds = applicationSecret.getElasticsearchCredentials();
    if (creds == null) {
      throw new IllegalStateException(
          "elasticsearch_credentials must be set in the application secret (AWS Secrets Manager JSON)");
    }
    if (StringUtil.isNullOrEmpty(creds.getUris())) {
      throw new IllegalStateException("elasticsearch_credentials.uris must be set");
    }
    if (StringUtil.isNullOrEmpty(creds.getIndexName())) {
      throw new IllegalStateException("elasticsearch_credentials.index_name must be set");
    }

    String[] uriStrings = creds.getUris().split(",");
    HttpHost[] hosts = new HttpHost[uriStrings.length];
    for (int i = 0; i < uriStrings.length; i++) {
      hosts[i] = HttpHost.create(uriStrings[i].trim());
    }

    RestClientBuilder builder = RestClient.builder(hosts);

    if (!StringUtil.isNullOrEmpty(creds.getApiKey())) {
      builder.setDefaultHeaders(new Header[]{new BasicHeader("Authorization", "ApiKey " + creds.getApiKey().trim())});
    } else if (!StringUtil.isNullOrEmpty(creds.getUsername()) && creds.getPassword() != null) {
      BasicCredentialsProvider provider = new BasicCredentialsProvider();
      provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(creds.getUsername(), creds.getPassword()));
      builder.setHttpClientConfigCallback(
          httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(provider));
    } else {
      throw new IllegalStateException(
          "Configure elasticsearch_credentials.api_key or username and password in application secret");
    }

    return builder.build();
  }

  @Bean
  public ElasticsearchClient elasticsearchClient(RestClient elasticsearchRestClient) {
    RestClientTransport transport =
        new RestClientTransport(elasticsearchRestClient, new JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
  }
}
