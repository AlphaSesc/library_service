package com.example.library_service.service;

import com.example.library_service.dto.BorrowRequest;
import com.example.library_service.dto.BorrowResponse;
import com.example.library_service.entity.Book;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LoanStatus;
import com.example.library_service.exception.InactiveUserException;
import com.example.library_service.exception.NoAvailableCopiesException;
import com.example.library_service.exception.ResourceAlreadyExistsException;
import com.example.library_service.exception.ResourceNotFoundException;
import com.example.library_service.repository.BookRepository;
import com.example.library_service.repository.LoanRepository;
import com.example.library_service.util.LoanStatusResolver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BorrowService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final AuthenticatedLibraryUserService authenticatedLibraryUserService;

    @Transactional
    public BorrowResponse borrowBook(BorrowRequest request) {
        LibraryUser libraryUser = authenticatedLibraryUserService.getCurrentStudentUser();

        if (!libraryUser.isActive()) {
            throw new InactiveUserException("Inactive library user cannot borrow books");
        }

        Book book = bookRepository.findByIsbn(request.getIsbn())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        if (!book.isActive()) {
            throw new ResourceNotFoundException("Book not found");
        }

        if (book.getAvailableCopies() <= 0) {
            throw new NoAvailableCopiesException("No available copies for this book");
        }

        loanRepository.findByLibraryUserAndBookAndReturnedAtIsNull(libraryUser, book)
                .ifPresent(loan -> {
                    throw new ResourceAlreadyExistsException("You have already borrowed this book");
                });

        Loan loan = Loan.builder()
                .libraryUser(libraryUser)
                .book(book)
                .build();

        Loan savedLoan = loanRepository.save(loan);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        return BorrowResponse.builder()
                .loanId(savedLoan.getId())
                .studentId(libraryUser.getStudentId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .status(LoanStatusResolver.resolve(savedLoan))
                .borrowedAt(savedLoan.getBorrowedAt())
                .dueAt(savedLoan.getDueAt())
                .build();
    }
}