package com.example.library_service.exception;

import org.springframework.http.HttpStatus;

public class InactiveUserException extends BusinessException {

    public InactiveUserException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}