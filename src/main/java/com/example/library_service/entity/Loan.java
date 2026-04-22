package com.example.library_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Entity representing a book loan transaction in the library
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to the user who borrowed the book
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "library_user_id", nullable = false)
    private LibraryUser libraryUser;

    // Reference to the borrowed book
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    // Timestamp when the book was borrowed
    @Column(nullable = false)
    private LocalDateTime borrowedAt;

    // Due date for returning the book
    @Column(nullable = false)
    private LocalDateTime dueAt;

    // Timestamp when the book was returned (null if not returned yet)
    private LocalDateTime returnedAt;

    // Indicates whether the loan is currently active
    @Column(nullable = false)
    private boolean active;

    // Timestamp when loan record was created
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Timestamp when loan record was last updated
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    // Initializes timestamps and default loan values before saving
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;

        // Set borrow time if not provided
        if (this.borrowedAt == null) {
            this.borrowedAt = LocalDateTime.now();
        }

        // Default loan period is 14 days if not specified
        if (this.dueAt == null) {
            this.dueAt = this.borrowedAt.plusDays(14);
        }
    }

    @PreUpdate
    // Updates timestamp before entity update
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}