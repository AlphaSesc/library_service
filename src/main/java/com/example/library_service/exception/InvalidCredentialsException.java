package com.example.library_service.exception;

import org.springframework.http.HttpStatus;

// Thrown when authentication fails due to invalid login credentials (HTTP 401)
public class InvalidCredentialsException extends BusinessException {
    public InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}