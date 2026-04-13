package com.project.ashutosh.config;

import com.project.ashutosh.model.ApplicationSecret;
import com.project.ashutosh.model.DatabaseCredentials;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DatabaseConfig {

  @Value("${spring.datasource.hikari.maximum-pool-size}")
  private int MAXIMUM_POOL_SIZE;

  @Value("${spring.datasource.hikari.minimum-idle}")
  private int MINIMUM_IDLE;

  @Value("${spring.datasource.hikari.connection-timeout}")
  private long CONNECTION_TIMEOUT_MS;

  @Value("${spring.datasource.hikari.idle-timeout}")
  private long IDLE_TIMEOUT_MS;

  @Value("${spring.datasource.hikari.max-lifetime}")
  private long MAX_LIFETIME_MS;

  @Bean
  @Primary
  public DataSource dataSource(ApplicationSecret applicationSecret) {
    DatabaseCredentials db = applicationSecret.getDatabaseCredentials();
    if (db == null || db.getUrl() == null || db.getUrl().isBlank()) {
      throw new IllegalStateException();
    }
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(db.getUrl().trim());
    ds.setUsername(db.getUsername());
    ds.setPassword(db.getPassword());
    ds.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
    ds.setMinimumIdle(MINIMUM_IDLE);
    ds.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
    ds.setIdleTimeout(IDLE_TIMEOUT_MS);
    ds.setMaxLifetime(MAX_LIFETIME_MS);
    ds.setPoolName("docsense-pool");
    return ds;
  }
}
