package com.example.library_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowRequest {

    @NotBlank(message = "ISBN is required")
    private String isbn;
}