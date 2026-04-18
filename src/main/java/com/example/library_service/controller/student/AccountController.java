package com.example.library_service.controller.student;

import com.example.library_service.dto.LoanHistoryResponse;
import com.example.library_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public List<LoanHistoryResponse> getMyBorrowingHistory() {
        return accountService.getMyBorrowingHistory();
    }
}