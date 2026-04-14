package com.example.library_service.controller;

import com.example.library_service.dto.ReturnRequest;
import com.example.library_service.dto.ReturnResponse;
import com.example.library_service.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping
    public ReturnResponse returnBook(@Valid @RequestBody ReturnRequest request) {
        return returnService.returnBook(request);
    }
}