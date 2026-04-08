package com.example.library_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LibraryRegisterRequest {

    @NotBlank(message = "Student ID is required")
    private String studentId;
}