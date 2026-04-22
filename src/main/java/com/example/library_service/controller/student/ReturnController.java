package com.example.library_service.controller.student;

import com.example.library_service.dto.ReturnRequest;
import com.example.library_service.dto.ReturnResponse;
import com.example.library_service.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/library/returns")
@RequiredArgsConstructor
// Controller handling book return operations for students
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    // Allows authenticated student to return a borrowed book using ISBN
    public ReturnResponse returnBook(@Valid @RequestBody ReturnRequest request) {
        return returnService.returnBook(request);
    }
}