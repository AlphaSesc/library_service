package com.example.library_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "books")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Entity representing a book in the library with inventory tracking
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique ISBN identifier for each book
    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    // Total number of copies owned by the library
    @Column(nullable = false)
    private int totalCopies;

    // Number of copies currently available for borrowing
    @Column(nullable = false)
    private int availableCopies;

    // Indicates whether the book is active (soft delete / visibility control)
    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    // Initializes timestamps and default values before saving new entity
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;

        // Ensures available copies default to total copies on creation
        if (this.availableCopies == 0) {
            this.availableCopies = this.totalCopies;
        }
    }

    @PreUpdate
    // Updates timestamp before entity update
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}