package com.example.library_service.exception;

import org.springframework.http.HttpStatus;

// Thrown when an external microservice (e.g., Finance) fails or is unavailable (HTTP 502)
public class ExternalServiceException extends BusinessException {

    public ExternalServiceException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}