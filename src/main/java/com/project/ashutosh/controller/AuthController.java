package com.project.ashutosh.controller;

import com.project.ashutosh.dto.AuthResponse;
import com.project.ashutosh.dto.LoginRequest;
import com.project.ashutosh.dto.RegisterRequest;
import com.project.ashutosh.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public AuthResponse login(@RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }
}
