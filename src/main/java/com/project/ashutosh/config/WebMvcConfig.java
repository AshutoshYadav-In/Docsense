package com.project.ashutosh.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  private final UserApiLoggingInterceptor userApiLoggingInterceptor;

  public WebMvcConfig(UserApiLoggingInterceptor userApiLoggingInterceptor) {
    this.userApiLoggingInterceptor = userApiLoggingInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(userApiLoggingInterceptor).addPathPatterns("/api/users/**");
  }
}
