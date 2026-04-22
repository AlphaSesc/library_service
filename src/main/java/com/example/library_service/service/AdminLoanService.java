package com.example.library_service.service;

import com.example.library_service.dto.LoanHistoryResponse;
import com.example.library_service.dto.StudentLoanSummaryResponse;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.repository.LibraryUserRepository;
import com.example.library_service.repository.LoanRepository;
import com.example.library_service.util.LoanStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
// Service providing admin-level access to loan monitoring and summary information
public class AdminLoanService {

    private final LoanRepository loanRepository;
    private final LibraryUserRepository libraryUserRepository;
    private final AuthenticatedLibraryUserService authenticatedLibraryUserService;

    // Retrieves all currently active loans in the system
    public List<LoanHistoryResponse> getCurrentLoans() {
        authenticatedLibraryUserService.getCurrentAdminUser();

        return loanRepository.findByReturnedAtIsNullOrderByBorrowedAtDesc()
                .stream()
                .map(this::mapToLoanHistoryResponse)
                .toList();
    }

    // Retrieves all overdue active loans in the system
    public List<LoanHistoryResponse> getOverdueLoans() {
        authenticatedLibraryUserService.getCurrentAdminUser();

        return loanRepository.findByReturnedAtIsNullAndDueAtBeforeOrderByBorrowedAtDesc(LocalDateTime.now())
                .stream()
                .map(this::mapToLoanHistoryResponse)
                .toList();
    }

    // Provides per-student summary of active and overdue loans for admin monitoring
    public List<StudentLoanSummaryResponse> getStudentLoanSummaries() {
        authenticatedLibraryUserService.getCurrentAdminUser();

        return libraryUserRepository.findByRole(LibraryRole.STUDENT)
                .stream()
                .map(user -> StudentLoanSummaryResponse.builder()
                        .studentId(user.getStudentId())
                        .booksOnLoan(
                                loanRepository.countByLibraryUserAndReturnedAtIsNull(user)
                        )
                        .overdueBooks(
                                loanRepository.countByLibraryUserAndReturnedAtIsNullAndDueAtBefore(
                                        user,
                                        LocalDateTime.now()
                                )
                        )
                        .build())
                .toList();
    }

    // Maps Loan entity to admin-facing loan history response
    private LoanHistoryResponse mapToLoanHistoryResponse(com.example.library_service.entity.Loan loan) {
        return LoanHistoryResponse.builder()
                .loanId(loan.getId())
                .studentId(loan.getLibraryUser().getStudentId())
                .isbn(loan.getBook().getIsbn())
                .title(loan.getBook().getTitle())
                .author(loan.getBook().getAuthor())
                .status(LoanStatusResolver.resolve(loan))
                .borrowedAt(loan.getBorrowedAt())
                .dueAt(loan.getDueAt())
                .returnedAt(loan.getReturnedAt())
                .build();
    }
}