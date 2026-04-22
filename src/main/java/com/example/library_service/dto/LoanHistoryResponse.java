package com.example.library_service.dto;

import com.example.library_service.entity.LoanStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Response DTO representing complete loan history of a user
public class LoanHistoryResponse {

    private Long loanId;
    private String studentId;
    private String isbn;
    private String title;
    private String author;
    private LoanStatus status;
    private LocalDateTime borrowedAt;
    private LocalDateTime dueAt;
    private LocalDateTime returnedAt;
}