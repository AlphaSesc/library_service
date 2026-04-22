package com.example.library_service.dto;

import com.example.library_service.entity.LoanStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Response DTO representing details after a book return operation
public class ReturnResponse {

    private Long loanId;
    private String studentId;
    private String isbn;
    private String title;
    // Updated status of the loan
    private LoanStatus status;
    private LocalDateTime borrowedAt;
    private LocalDateTime dueAt;
    // Timestamp when the book was returned (marks loan completion)
    private LocalDateTime returnedAt;
}