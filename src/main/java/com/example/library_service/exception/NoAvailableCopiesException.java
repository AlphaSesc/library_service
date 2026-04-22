package com.example.library_service.exception;

import org.springframework.http.HttpStatus;

// Thrown when a book cannot be borrowed due to no available copies
public class NoAvailableCopiesException extends BusinessException {

    public NoAvailableCopiesException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}