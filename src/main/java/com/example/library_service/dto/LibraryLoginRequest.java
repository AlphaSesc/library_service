package com.example.library_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LibraryLoginRequest {

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "PIN is required")
    private String pin;
}