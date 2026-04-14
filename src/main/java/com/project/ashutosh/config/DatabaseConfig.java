package com.project.ashutosh.config;

import ch.qos.logback.core.util.StringUtil;
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
  public int MAXIMUM_POOL_SIZE;

  @Value("${spring.datasource.hikari.minimum-idle}")
  public int MINIMUM_IDLE;

  @Value("${spring.datasource.hikari.connection-timeout}")
  public long CONNECTION_TIMEOUT_MS;

  @Value("${spring.datasource.hikari.idle-timeout}")
  public long IDLE_TIMEOUT_MS;

  @Value("${spring.datasource.hikari.max-lifetime}")
  public long MAX_LIFETIME_MS;

  @Bean
  @Primary
  public DataSource dataSource(ApplicationSecret applicationSecret) {
    DatabaseCredentials db = applicationSecret.getDatabaseCredentials();
    if (db == null || StringUtil.isNullOrEmpty(db.getUrl())) {
      throw new IllegalStateException();
    }
    HikariDataSource ds = new HikariDataSource();
    ds.setJdbcUrl(db.getUrl());
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
