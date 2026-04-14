package com.example.library_service.dto;

import com.example.library_service.entity.LoanStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BorrowResponse {

    private Long loanId;
    private String studentId;
    private String isbn;
    private String title;
    private LoanStatus status;
    private LocalDateTime borrowedAt;
    private LocalDateTime dueAt;
}