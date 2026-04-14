package com.example.library_service.exception;

import org.springframework.http.HttpStatus;

public class NoAvailableCopiesException extends BusinessException {

    public NoAvailableCopiesException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}