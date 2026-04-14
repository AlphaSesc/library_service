package com.example.library_service.controller;

import com.example.library_service.dto.AddBookRequest;
import com.example.library_service.dto.BookResponse;
import com.example.library_service.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public List<BookResponse> getAllBooks() {
        return bookService.getAllBooks();
    }

    @PostMapping
    public BookResponse addBook(@Valid @RequestBody AddBookRequest request) {
        return bookService.addBook(request);
    }
}