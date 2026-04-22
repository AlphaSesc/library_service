package com.example.library_service.repository;

import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

// Repository for managing loan (borrow/return) operations
public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Retrieves all loans of a user, sorted by most recent borrow
    List<Loan> findByLibraryUserOrderByBorrowedAtDesc(LibraryUser libraryUser);

    // Finds active loan for a specific book (used to prevent duplicate borrow)
    Optional<Loan> findByLibraryUserAndBookAndReturnedAtIsNull(LibraryUser libraryUser, Book book);

    // Retrieves all currently active (not returned) loans
    List<Loan> findByReturnedAtIsNullOrderByBorrowedAtDesc();

    // Counts number of active loans for a user
    long countByLibraryUserAndReturnedAtIsNull(LibraryUser libraryUser);

    // Counts overdue active loans for a user
    long countByLibraryUserAndReturnedAtIsNullAndDueAtBefore(
            LibraryUser libraryUser,
            LocalDateTime time
    );

    // Retrieves all overdue loans in the system
    List<Loan> findByReturnedAtIsNullAndDueAtBeforeOrderByBorrowedAtDesc(LocalDateTime time);

    // Retrieves active loans of a user
    List<Loan> findByLibraryUserAndReturnedAtIsNullOrderByBorrowedAtDesc(LibraryUser libraryUser);
}