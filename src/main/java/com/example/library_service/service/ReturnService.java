package com.example.library_service.service;

import com.example.library_service.client.FinanceClient;
import com.example.library_service.dto.ReturnRequest;
import com.example.library_service.dto.ReturnResponse;
import com.example.library_service.dto.finance.CreateInvoiceRequest;
import com.example.library_service.dto.finance.InvoiceType;
import com.example.library_service.entity.Book;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LoanStatus;
import com.example.library_service.exception.InactiveUserException;
import com.example.library_service.exception.ResourceNotFoundException;
import com.example.library_service.repository.BookRepository;
import com.example.library_service.repository.LoanRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReturnService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final AuthenticatedLibraryUserService authenticatedLibraryUserService;
    private final FinanceClient financeClient;

    @Transactional
    public ReturnResponse returnBook(ReturnRequest request) {
        LibraryUser libraryUser = authenticatedLibraryUserService.getCurrentStudentUser();

        if (!libraryUser.isActive()) {
            throw new InactiveUserException("Inactive library user cannot return books");
        }

        Book book = bookRepository.findByIsbn(request.getIsbn())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        Loan loan = loanRepository.findByLibraryUserAndBookAndStatus(libraryUser, book, LoanStatus.BORROWED)
                .orElseThrow(() -> new ResourceNotFoundException("Active loan not found for this book"));

        LocalDateTime returnedAt = LocalDateTime.now();
        boolean returnedLate = returnedAt.isAfter(loan.getDueAt());

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnedAt(LocalDateTime.now());

        Loan savedLoan = loanRepository.save(loan);

        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

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

        return ReturnResponse.builder()
                .loanId(savedLoan.getId())
                .studentId(libraryUser.getStudentId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .status(savedLoan.getStatus())
                .borrowedAt(savedLoan.getBorrowedAt())
                .dueAt(savedLoan.getDueAt())
                .returnedAt(savedLoan.getReturnedAt())
                .build();
    }
}