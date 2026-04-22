package com.example.library_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Request DTO for registering a student in the library system
public class LibraryRegisterRequest {

    // Student ID received from Student Portal service
    @NotBlank(message = "Student ID is required")
    private String studentId;
}