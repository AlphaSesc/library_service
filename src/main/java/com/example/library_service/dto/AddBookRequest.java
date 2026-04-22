package com.example.library_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Request DTO for adding a new book with basic validation
public class AddBookRequest {

    // ISBN must be provided and unique
    @NotBlank(message = "ISBN is required")
    private String isbn;

    // Book title is mandatory
    @NotBlank(message = "Title is required")
    private String title;

    // Author name is mandatory
    @NotBlank(message = "Author is required")
    private String author;

    // At least one copy must exist in library
    @Min(value = 1, message = "Total copies must be at least 1")
    private int totalCopies;
}