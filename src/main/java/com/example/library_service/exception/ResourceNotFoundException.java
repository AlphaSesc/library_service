package com.example.library_service.exception;

import org.springframework.http.HttpStatus;

// Thrown when a requested resource cannot be found (HTTP 404)
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}