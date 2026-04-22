package com.example.library_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Response DTO providing a summary of a student's loan status
public class StudentLoanSummaryResponse {

    private String studentId;
    // Number of books currently borrowed (active loans)
    private long booksOnLoan;
    // Number of overdue books
    private long overdueBooks;
}