package com.example.library_service.repository;

import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    // Borrowing history of a student
    List<Loan> findByLibraryUser(LibraryUser libraryUser);

    List<Loan> findByLibraryUserOrderByBorrowedAtDesc(LibraryUser libraryUser);

    Optional<Loan> findByLibraryUserAndBookAndReturnedAtIsNull(LibraryUser libraryUser, Book book);

    List<Loan> findByReturnedAtIsNullOrderByBorrowedAtDesc();

    long countByLibraryUserAndReturnedAtIsNull(LibraryUser libraryUser);

    long countByLibraryUserAndReturnedAtIsNullAndDueAtBefore(
            LibraryUser libraryUser,
            LocalDateTime time
    );

    List<Loan> findByReturnedAtIsNullAndDueAtBeforeOrderByBorrowedAtDesc(LocalDateTime time);
}