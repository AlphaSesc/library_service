package com.example.library_service.exception;

import org.springframework.http.HttpStatus;

// Thrown when a user attempts an operation without sufficient permissions (HTTP 403)
public class UnauthorizedOperationException extends BusinessException {
    public UnauthorizedOperationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}