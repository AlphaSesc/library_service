package com.example.library_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Request DTO for library user authentication using studentId and PIN
public class LibraryLoginRequest {

    // Unique identifier linking user to Student Portal
    @NotBlank(message = "Student ID is required")
    private String studentId;

    // PIN used for library authentication
    @NotBlank(message = "PIN is required")
    private String pin;
}