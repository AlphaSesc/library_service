package com.example.library_service.controller;

import com.example.library_service.dto.LoanHistoryResponse;
import com.example.library_service.dto.StudentLoanSummaryResponse;
import com.example.library_service.service.AdminLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminLoanController {

    private final AdminLoanService adminLoanService;

    @GetMapping("/loans/current")
    public List<LoanHistoryResponse> getCurrentLoans() {
        return adminLoanService.getCurrentLoans();
    }

    @GetMapping("/loans/overdue")
    public List<LoanHistoryResponse> getOverdueLoans() {
        return adminLoanService.getOverdueLoans();
    }

    @GetMapping("/students/loan-summary")
    public List<StudentLoanSummaryResponse> getStudentLoanSummaries() {
        return adminLoanService.getStudentLoanSummaries();
    }
}