package com.example.library_service.exception;

import org.springframework.http.HttpStatus;

public class UnauthorizedOperationException extends BusinessException {
    public UnauthorizedOperationException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}