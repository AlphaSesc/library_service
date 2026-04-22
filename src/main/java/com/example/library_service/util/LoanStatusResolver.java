package com.example.library_service.util;

import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LoanStatus;

import java.time.LocalDateTime;

// Utility class for determining the current status of a loan
public final class LoanStatusResolver {

    private LoanStatusResolver() {
    }

    // Resolves loan status based on return time and due date
    public static LoanStatus resolve(Loan loan) {

        // If book has been returned → status is RETURNED
        if (loan.getReturnedAt() != null) {
            return LoanStatus.RETURNED;
        }

        // If current time is past due date → status is OVERDUE
        if (loan.getDueAt().isBefore(LocalDateTime.now())) {
            return LoanStatus.OVERDUE;
        }

        // Otherwise → book is currently borrowed
        return LoanStatus.BORROWED;
    }
}