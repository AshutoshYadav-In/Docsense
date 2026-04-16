package com.project.ashutosh.config;

import com.project.ashutosh.secret.ApplicationSecret;
import com.project.ashutosh.security.ApiPathPatterns;
import com.project.ashutosh.security.InternalClientAuthenticationFilter;
import com.project.ashutosh.security.JwtAuthenticationFilter;
import com.project.ashutosh.security.JwtService;
import com.project.ashutosh.security.TenantResolutionFilter;
import com.project.ashutosh.service.TenantMembershipService;
import com.project.ashutosh.tenant.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(
      JwtService jwtService, ApiPathPatterns apiPathPatterns) {
    return new JwtAuthenticationFilter(jwtService, apiPathPatterns);
  }

  @Bean
  public InternalClientAuthenticationFilter internalClientAuthenticationFilter(
      ApplicationSecret applicationSecret, ApiPathPatterns apiPathPatterns) {
    return new InternalClientAuthenticationFilter(applicationSecret, apiPathPatterns);
  }

  @Bean
  public TenantResolutionFilter tenantResolutionFilter(
      TenantContext tenantContext,
      TenantMembershipService tenantMembershipService,
      ApiPathPatterns apiPathPatterns) {
    return new TenantResolutionFilter(tenantContext, tenantMembershipService, apiPathPatterns);
  }

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      InternalClientAuthenticationFilter internalClientAuthenticationFilter,
      TenantResolutionFilter tenantResolutionFilter)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable);

    http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    http.addFilterAfter(internalClientAuthenticationFilter, JwtAuthenticationFilter.class);
    http.addFilterAfter(tenantResolutionFilter, InternalClientAuthenticationFilter.class);

    http.authorizeHttpRequests(
        auth ->
            auth.requestMatchers("/api/auth", "/api/auth/**")
                .permitAll()
                .requestMatchers("/api/internal", "/api/internal/**")
                .authenticated()
                .requestMatchers("/api/**")
                .authenticated()
                .anyRequest()
                .permitAll());
    return http.build();
  }
}
