package com.example.library_service.exception;

import org.springframework.http.HttpStatus;

// Thrown when an operation is attempted by an inactive library user
public class InactiveUserException extends BusinessException {

    public InactiveUserException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}