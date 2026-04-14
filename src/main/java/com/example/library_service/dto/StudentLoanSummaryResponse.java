package com.example.library_service.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentLoanSummaryResponse {

    private String studentId;
    private long booksOnLoan;
    private long overdueBooks;
}