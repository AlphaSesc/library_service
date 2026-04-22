package com.example.library_service.repository;

import com.example.library_service.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

// Repository for accessing and querying Book entities
public interface BookRepository extends JpaRepository<Book, Long> {

    // Finds a book by its unique ISBN
    Optional<Book> findByIsbn(String isbn);

    // Checks if a book with given ISBN already exists (used to prevent duplicates)
    boolean existsByIsbn(String isbn);

    // Retrieves only active (non-deleted) books
    List<Book> findByActiveTrue();
}