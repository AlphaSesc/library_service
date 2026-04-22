package com.example.library_service.service;

import com.example.library_service.dto.AddBookRequest;
import com.example.library_service.dto.BookResponse;
import com.example.library_service.entity.Book;
import com.example.library_service.exception.ResourceAlreadyExistsException;
import com.example.library_service.repository.BookRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
// Service responsible for managing books and inventory in the library
public class BookService {

    private final BookRepository bookRepository;

    // Retrieves all active books and maps them to response DTOs
    public List<BookResponse> getAllBooks() {
        return bookRepository.findByActiveTrue()
                .stream()
                .map(this::mapToBookResponse)
                .toList();
    }

    @Transactional
    // Adds a new book to the library after validating uniqueness of ISBN
    public BookResponse addBook(AddBookRequest request) {

        // Prevent duplicate books with same ISBN
        bookRepository.findByIsbn(request.getIsbn())
                .ifPresent(book -> {
                    throw new ResourceAlreadyExistsException("Book with this ISBN already exists");
                });

        // Create new book entity with initial inventory
        Book book = Book.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .author(request.getAuthor())
                .totalCopies(request.getTotalCopies())
                .availableCopies(request.getTotalCopies())
                .build();

        Book savedBook = bookRepository.save(book);

        return mapToBookResponse(savedBook);
    }

    // Maps Book entity to response DTO
    private BookResponse mapToBookResponse(Book book) {
        return BookResponse.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .author(book.getAuthor())
                .totalCopies(book.getTotalCopies())
                .availableCopies(book.getAvailableCopies())
                .active(book.isActive())
                .build();
    }
}