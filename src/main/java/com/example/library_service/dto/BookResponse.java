package com.example.library_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Response DTO representing book details sent to clients
public class BookResponse {

    private Long id;
    private String isbn;
    private String title;
    private String author;
    private int totalCopies;
    private int availableCopies;
    private boolean active;
}