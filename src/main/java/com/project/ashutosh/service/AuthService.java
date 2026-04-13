package com.project.ashutosh.service;

import com.project.ashutosh.dao.UserDao;
import com.project.ashutosh.dto.AuthResponse;
import com.project.ashutosh.dto.LoginRequest;
import com.project.ashutosh.dto.RegisterRequest;
import com.project.ashutosh.entity.User;
import com.project.ashutosh.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserDao userDao;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final long expirationMs;

  public AuthService(UserDao userDao, PasswordEncoder passwordEncoder, JwtService jwtService,
      @Value("${jwt.expiration-ms}") long expirationMs) {
    this.userDao = userDao;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.expirationMs = expirationMs;
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    User user = userDao.findByEmail(request.getEmail())
        .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid email or password");
    }
    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthResponse(token, "Bearer", expirationMs);
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (userDao.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException("Email already registered");
    }
    User user = new User();
    user.setEmail(request.getEmail());
    user.setName(request.getName());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    userDao.save(user);
    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthResponse(token, "Bearer", expirationMs);
  }
}
