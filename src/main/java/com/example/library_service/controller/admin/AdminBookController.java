package com.example.library_service.controller.admin;

import com.example.library_service.dto.AddBookRequest;
import com.example.library_service.dto.BookResponse;
import com.example.library_service.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/library/books")
@RequiredArgsConstructor
// Controller providing admin-level operations for managing books
public class AdminBookController {

    private final BookService bookService;

    // Returns all active books (admin view)
    @GetMapping
    public List<BookResponse> getAllBooksForAdmin() {
        return bookService.getAllBooks();
    }

    // Allows admin to add new books to the library inventory
    @PostMapping
    public BookResponse addBook(@Valid @RequestBody AddBookRequest request) {
        return bookService.addBook(request);
    }
}