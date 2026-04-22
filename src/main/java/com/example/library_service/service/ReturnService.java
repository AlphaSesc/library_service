package com.example.library_service.service;

import com.example.library_service.client.FinanceClient;
import com.example.library_service.dto.ReturnRequest;
import com.example.library_service.dto.ReturnResponse;
import com.example.library_service.dto.finance.CreateInvoiceRequest;
import com.example.library_service.dto.finance.InvoiceType;
import com.example.library_service.entity.Book;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Loan;
import com.example.library_service.exception.InactiveUserException;
import com.example.library_service.exception.ResourceNotFoundException;
import com.example.library_service.repository.BookRepository;
import com.example.library_service.repository.LoanRepository;
import com.example.library_service.util.LoanStatusResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
// Service responsible for book return logic, inventory update, and late fine handling
public class ReturnService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final AuthenticatedLibraryUserService authenticatedLibraryUserService;
    private final FinanceClient financeClient;

    @Transactional
    // Returns a borrowed book for the currently authenticated library user
    public ReturnResponse returnBook(ReturnRequest request) {

        // Retrieve authenticated user and ensure they are active
        LibraryUser libraryUser = authenticatedLibraryUserService.getCurrentStudentUser();

        if (!libraryUser.isActive()) {
            throw new InactiveUserException("Inactive library user cannot return books");
        }

        // Fetch target book by ISBN
        Book book = bookRepository.findByIsbn(request.getIsbn())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        // Fetch active loan for this user and book
        Loan loan = loanRepository.findByLibraryUserAndBookAndReturnedAtIsNull(
                libraryUser,
                book
        ).orElseThrow(() -> new ResourceNotFoundException("Active loan not found for this book"));

        // Mark return time and determine whether the book is returned late
        LocalDateTime returnedAt = LocalDateTime.now();
        boolean returnedLate = returnedAt.isAfter(loan.getDueAt());

        loan.setReturnedAt(returnedAt);

        Loan savedLoan = loanRepository.save(loan);

        // Increase available inventory after successful return
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        // Generate fine invoice through Finance service if return is late
        if (returnedLate) {
            financeClient.createInvoice(
                    CreateInvoiceRequest.builder()
                            .studentId(libraryUser.getStudentId())
                            .courseCode(null)
                            .amount(BigDecimal.valueOf(500.0))
                            .invoiceType(InvoiceType.LIBRARY_FINE)
                            .build()
            );
        }

        // Return updated loan details including resolved status
        return ReturnResponse.builder()
                .loanId(savedLoan.getId())
                .studentId(libraryUser.getStudentId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .status(LoanStatusResolver.resolve(savedLoan))
                .borrowedAt(savedLoan.getBorrowedAt())
                .dueAt(savedLoan.getDueAt())
                .returnedAt(savedLoan.getReturnedAt())
                .build();
    }
}