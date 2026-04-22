package com.example.library_service.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
// Standardized error response structure used across all APIs
public class ApiErrorResponse {

    // Timestamp when the error occurred
    private LocalDateTime timestamp;

    // HTTP status code (e.g., 400, 404, 500)
    private int status;

    // Short error name (e.g., Bad Request, Not Found)
    private String error;

    // Detailed error message for client understanding
    private String message;

    // API path where the error occurred
    private String path;

    // Field-level validation errors (if any)
    private Map<String, String> validationErrors;
}