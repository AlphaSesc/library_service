package com.example.library_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "library_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
// Entity representing a library system user associated with a student
public class LibraryUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique identifier linking user to Student Portal
    @Column(nullable = false, unique = true)
    private String studentId;

    // Hashed PIN used for authentication in library system
    @Column(nullable = false)
    private String pinHash;

    // Role of the library user (e.g., STUDENT, ADMIN)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LibraryRole role;

    // Indicates if user is logging in for the first time
    @Column(nullable = false)
    private boolean firstLogin;

    // Soft delete / active status flag
    @Column(nullable = false)
    private boolean active;

    // Timestamp when user was created
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    // Initializes timestamps and default active status on creation
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.active = true;
    }

    // Updates timestamp before entity update
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}