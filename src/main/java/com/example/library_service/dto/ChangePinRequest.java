package com.example.library_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePinRequest {

    @NotBlank(message = "Old PIN is required")
    private String oldPin;

    @NotBlank(message = "New PIN is required")
    private String newPin;
}