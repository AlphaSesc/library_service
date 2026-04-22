package com.example.library_service.controller.admin;

import com.example.library_service.dto.LoanHistoryResponse;
import com.example.library_service.dto.StudentLoanSummaryResponse;
import com.example.library_service.service.AdminLoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/library")
@RequiredArgsConstructor
// Controller providing admin-level endpoints for monitoring loans and student activity
public class AdminLoanController {

    private final AdminLoanService adminLoanService;

    // Returns all currently active loans across the system
    @GetMapping("/loans/current")
    public List<LoanHistoryResponse> getCurrentLoans() {
        return adminLoanService.getCurrentLoans();
    }

    // Returns all overdue loans for tracking late returns
    @GetMapping("/loans/overdue")
    public List<LoanHistoryResponse> getOverdueLoans() {
        return adminLoanService.getOverdueLoans();
    }

    // Provides per-student summary of active and overdue loans
    @GetMapping("/students/loan-summary")
    public List<StudentLoanSummaryResponse> getStudentLoanSummaries() {
        return adminLoanService.getStudentLoanSummaries();
    }
}