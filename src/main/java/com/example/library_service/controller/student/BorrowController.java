package com.example.library_service.controller.student;

import com.example.library_service.dto.BorrowRequest;
import com.example.library_service.dto.BorrowResponse;
import com.example.library_service.service.BorrowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/library/borrow")
@RequiredArgsConstructor
// Controller handling book borrowing operations for students
public class BorrowController {

    private final BorrowService borrowService;

    @PostMapping
    // Allows authenticated student to borrow a book using ISBN
    public BorrowResponse borrowBook(@Valid @RequestBody BorrowRequest request) {
        return borrowService.borrowBook(request);
    }
}