package com.project.ashutosh.config;

import ch.qos.logback.core.util.StringUtil;
import com.project.ashutosh.model.ApplicationSecret;
import com.project.ashutosh.model.ElasticsearchCredentials;
import java.net.URI;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;

/**
 * Amazon OpenSearch Service / Elasticsearch-compatible domains using <strong>IAM SigV4</strong>
 * signing (instance/task role, env keys, profile, etc.). No fine-grained master user/password.
 *
 * <p>Ensure the execution role is allowed in the domain access policy and has the appropriate
 * OpenSearch permissions (e.g. {@code es:ESHttp*} on the domain ARN).
 */
@Configuration
public class ElasticsearchConfig {

  @Value("${aws.region}")
  public String AWS_REGION;

  @Value("${aws.profile:}")
  public String AWS_PROFILE;

  @Bean
  public OpenSearchClient openSearchClient(
      ApplicationSecret applicationSecret) {
    ElasticsearchCredentials creds = applicationSecret.getElasticsearchCredentials();
    if (creds == null) {
      throw new IllegalStateException(
          "elasticsearch_credentials must be set in the application secret (AWS Secrets Manager JSON)");
    }
    if (StringUtil.isNullOrEmpty(creds.getUri())) {
      throw new IllegalStateException("elasticsearch_credentials.uri must be set");
    }
    if (StringUtil.isNullOrEmpty(AWS_REGION)) {
      throw new IllegalStateException("aws.region must be set for OpenSearch SigV4 signing");
    }

    URI endpoint = URI.create(creds.getUri().trim());
    String host = hostFromUri(endpoint);

    SdkHttpClient httpClient = ApacheHttpClient.builder().build();

    AwsCredentialsProvider credentialsProvider = credentialsProviderFor(AWS_PROFILE);

    AwsSdk2TransportOptions transportOptions =
        AwsSdk2TransportOptions.builder().setCredentials(credentialsProvider).build();

    AwsSdk2Transport transport =
        new AwsSdk2Transport(httpClient, host, Region.of(AWS_REGION), transportOptions);

    return new OpenSearchClient(transport);
  }

  private static AwsCredentialsProvider credentialsProviderFor(String awsProfile) {
    if (!StringUtil.isNullOrEmpty(awsProfile)) {
      return ProfileCredentialsProvider.builder().profileName(awsProfile).build();
    }
    return DefaultCredentialsProvider.builder().build();
  }

  /** Host (and non-default port if present) for {@link AwsSdk2Transport}. */
  private static String hostFromUri(URI uri) {
    String host = uri.getHost();
    if (host == null || host.isEmpty()) {
      throw new IllegalStateException("elasticsearch_credentials.uri must include a valid host");
    }
    int port = uri.getPort();
    if (port != -1) {
      return host + ":" + port;
    }
    return host;
  }
}
