package com.project.ashutosh.service;

import com.project.ashutosh.dao.UserDao;
import com.project.ashutosh.dto.request.LoginRequest;
import com.project.ashutosh.dto.request.RegisterRequest;
import com.project.ashutosh.dto.response.AuthResponse;
import com.project.ashutosh.entity.User;
import com.project.ashutosh.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {

  private final UserDao userDao;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(UserDao userDao, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userDao = userDao;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public AuthResponse login(LoginRequest request) {
    User user =
        userDao
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw new BadCredentialsException("Invalid email or password");
    }
    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthResponse(token, "Bearer", jwtService.getExpirationMs());
  }

  public AuthResponse register(RegisterRequest request) {
    if (userDao.existsByEmail(request.getEmail())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
    }
    User user = new User();
    user.setEmail(request.getEmail());
    user.setName(request.getName());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    userDao.save(user);
    String token = jwtService.generateToken(user.getId(), user.getEmail());
    return new AuthResponse(token, "Bearer", jwtService.getExpirationMs());
  }
}
