package com.example.library_service.repository;

import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LoanStatus;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Borrowing history of a student
    List<Loan> findByLibraryUser(LibraryUser libraryUser);

    List<Loan> findByLibraryUserOrderByBorrowedAtDesc(LibraryUser libraryUser);

    // Current active loans
    List<Loan> findByStatus(LoanStatus status);

    List<Loan> findByStatusOrderByBorrowedAtDesc(LoanStatus status);

    // Loans by status for a specific user
    List<Loan> findByLibraryUserAndStatus(LibraryUser libraryUser, LoanStatus status);

    // Loans for a specific book
    List<Loan> findByBook(Book book);

    // Active loans (not returned)
    List<Loan> findByActiveTrue();

    // Overdue loans
    List<Loan> findByStatus(LoanStatus status, boolean active);

    // Count loans by user and status (useful for admin view)
    long countByLibraryUserAndStatus(LibraryUser libraryUser, LoanStatus status);

    Optional<Loan> findByLibraryUserAndBookAndStatus(LibraryUser libraryUser, Book book, LoanStatus status);
}