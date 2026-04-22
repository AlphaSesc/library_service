package com.example.library_service.controller.student;

import com.example.library_service.dto.LoanHistoryResponse;
import com.example.library_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/library/account")
@RequiredArgsConstructor
// Controller exposing student-specific endpoints for viewing borrowings
public class AccountController {

    private final AccountService accountService;

    // Returns currently active borrowings of the authenticated student
    @GetMapping("/my-borrowings")
    public List<LoanHistoryResponse> getMyBorrowings() {
        return accountService.getMyBorrowings();
    }

    // Returns complete borrowing history (active + returned) of the authenticated student
    @GetMapping("/me")
    public List<LoanHistoryResponse> getMyBorrowingHistory() {
        return accountService.getMyBorrowingHistory();
    }
}