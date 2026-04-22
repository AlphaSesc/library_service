package com.example.library_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request DTO for returning a borrowed book using ISBN
public class ReturnRequest {

    // ISBN of the book to be returned
    @NotBlank(message = "ISBN is required")
    private String isbn;
}