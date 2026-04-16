package com.project.ashutosh.service;

import com.project.ashutosh.dao.UserDao;
import com.project.ashutosh.dto.request.CreateUserRequest;
import com.project.ashutosh.dto.response.UserResponse;
import com.project.ashutosh.entity.User;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * User persistence. For tenant-scoped queries, use {@link com.project.ashutosh.tenant.TenantContext}
 * (set by {@link com.project.ashutosh.security.TenantResolutionFilter} on {@code /api/users/**}).
 */
@Service
@Transactional
public class UserService {

  private final UserDao userDao;
  private final PasswordEncoder passwordEncoder;

  public UserService(UserDao userDao, PasswordEncoder passwordEncoder) {
    this.userDao = userDao;
    this.passwordEncoder = passwordEncoder;
  }

  public List<UserResponse> findAll() {
    return userDao.findAll().stream().map(this::toResponse).toList();
  }

  public UserResponse getUser(Long id) {
    return userDao
        .findById(id)
        .map(this::toResponse)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  public UserResponse createUser(CreateUserRequest request) {
    if (userDao.existsByEmail(request.getEmail())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
    }
    User user = new User();
    user.setEmail(request.getEmail());
    user.setName(request.getName());
    user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
    User saved = userDao.save(user);
    return toResponse(saved);
  }

  public void deleteUser(Long id) {
    if (userDao.findById(id).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    userDao.deleteById(id);
  }

  private UserResponse toResponse(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getName());
  }
}
