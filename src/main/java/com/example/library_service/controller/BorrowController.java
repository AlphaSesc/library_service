package com.example.library_service.controller;

import com.example.library_service.dto.BorrowRequest;
import com.example.library_service.dto.BorrowResponse;
import com.example.library_service.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @PostMapping
    public BorrowResponse borrowBook(@Valid @RequestBody BorrowRequest request) {
        return borrowService.borrowBook(request);
    }
}