package com.example.library_service.util;

import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LoanStatus;

import java.time.LocalDateTime;

public final class LoanStatusResolver {

    private LoanStatusResolver() {
    }

    public static LoanStatus resolve(Loan loan) {
        if (loan.getReturnedAt() != null) {
            return LoanStatus.RETURNED;
        }

        if (loan.getDueAt().isBefore(LocalDateTime.now())) {
            return LoanStatus.OVERDUE;
        }

        return LoanStatus.BORROWED;
    }
}