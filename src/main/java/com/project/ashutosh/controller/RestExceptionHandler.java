package com.project.ashutosh.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<Void> handleBadCredentials() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
