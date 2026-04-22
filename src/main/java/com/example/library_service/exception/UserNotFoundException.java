package com.example.library_service.exception;

import org.springframework.http.HttpStatus;

// Thrown when a requested user or related profile cannot be found (HTTP 404)
public class UserNotFoundException extends BusinessException {
    public UserNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}