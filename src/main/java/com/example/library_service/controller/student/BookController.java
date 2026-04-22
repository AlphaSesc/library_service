package com.example.library_service.controller.student;

import com.example.library_service.dto.AddBookRequest;
import com.example.library_service.dto.BookResponse;
import com.example.library_service.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/library/books")
@RequiredArgsConstructor
// Controller for retrieving available books for students
public class BookController {

    private final BookService bookService;

    // Returns list of all active books in the library
    @GetMapping
    public List<BookResponse> getAllBooks() {
        return bookService.getAllBooks();
    }

}