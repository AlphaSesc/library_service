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
public class BookService {

    private final BookRepository bookRepository;

    public List<BookResponse> getAllBooks() {
        return bookRepository.findByActiveTrue()
                .stream()
                .map(this::mapToBookResponse)
                .toList();
    }

    @Transactional
    public BookResponse addBook(AddBookRequest request) {
        bookRepository.findByIsbn(request.getIsbn())
                .ifPresent(book -> {
                    throw new ResourceAlreadyExistsException("Book with this ISBN already exists");
                });

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