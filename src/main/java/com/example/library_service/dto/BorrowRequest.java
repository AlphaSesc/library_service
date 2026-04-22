package com.example.library_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request DTO for borrowing a book identified by ISBN
public class BorrowRequest {

    @NotBlank(message = "ISBN is required")
    private String isbn;
}