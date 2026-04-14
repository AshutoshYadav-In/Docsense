package com.project.ashutosh.config;

import com.project.ashutosh.model.ApplicationSecret;
import com.project.ashutosh.security.InternalClientAuthenticationFilter;
import com.project.ashutosh.security.JwtAuthenticationFilter;
import com.project.ashutosh.security.JwtService;
import com.project.ashutosh.security.TenantResolutionFilter;
import com.project.ashutosh.service.TenantMembershipService;
import com.project.ashutosh.tenant.TenantContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
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
@EnableMethodSecurity
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService) {
    return new JwtAuthenticationFilter(jwtService);
  }

  @Bean
  public InternalClientAuthenticationFilter internalClientAuthenticationFilter(
      ApplicationSecret applicationSecret) {
    return new InternalClientAuthenticationFilter(applicationSecret);
  }

  @Bean
  public TenantResolutionFilter tenantResolutionFilter(
      TenantContext tenantContext, TenantMembershipService tenantMembershipService) {
    return new TenantResolutionFilter(tenantContext, tenantMembershipService);
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
            auth.requestMatchers("/api/auth/**")
                .permitAll()
                .requestMatchers("/api/internal/**")
                .authenticated()
                .requestMatchers("/api/users/**")
                .authenticated()
                .requestMatchers("/api/tenants/**")
                .authenticated()
                .anyRequest()
                .permitAll());
    return http.build();
  }
}
