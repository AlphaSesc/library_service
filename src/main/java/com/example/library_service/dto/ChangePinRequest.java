package com.example.library_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
// Request DTO for updating a user's library PIN
public class ChangePinRequest {

    // Current PIN (used to verify identity before change)
    @NotBlank(message = "Old PIN is required")
    private String oldPin;

    // New PIN to be set after successful validation
    @NotBlank(message = "New PIN is required")
    private String newPin;
}