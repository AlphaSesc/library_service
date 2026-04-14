package com.example.library_service.service;

import com.example.library_service.dto.LoanHistoryResponse;
import com.example.library_service.dto.StudentLoanSummaryResponse;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.LoanStatus;
import com.example.library_service.repository.LibraryUserRepository;
import com.example.library_service.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminLoanService {

    private final LoanRepository loanRepository;
    private final LibraryUserRepository libraryUserRepository;
    private final AuthenticatedLibraryUserService authenticatedLibraryUserService;

    public List<LoanHistoryResponse> getCurrentLoans() {
        authenticatedLibraryUserService.getCurrentAdminUser();

        return loanRepository.findByStatusOrderByBorrowedAtDesc(LoanStatus.BORROWED)
                .stream()
                .map(this::mapToLoanHistoryResponse)
                .toList();
    }

    public List<LoanHistoryResponse> getOverdueLoans() {
        authenticatedLibraryUserService.getCurrentAdminUser();

        return loanRepository.findByStatusOrderByBorrowedAtDesc(LoanStatus.OVERDUE)
                .stream()
                .map(this::mapToLoanHistoryResponse)
                .toList();
    }

    public List<StudentLoanSummaryResponse> getStudentLoanSummaries() {
        authenticatedLibraryUserService.getCurrentAdminUser();

        return libraryUserRepository.findByRole(LibraryRole.STUDENT)
                .stream()
                .map(user -> StudentLoanSummaryResponse.builder()
                        .studentId(user.getStudentId())
                        .booksOnLoan(loanRepository.countByLibraryUserAndStatus(user, LoanStatus.BORROWED))
                        .overdueBooks(loanRepository.countByLibraryUserAndStatus(user, LoanStatus.OVERDUE))
                        .build())
                .toList();
    }

    private LoanHistoryResponse mapToLoanHistoryResponse(com.example.library_service.entity.Loan loan) {
        return LoanHistoryResponse.builder()
                .loanId(loan.getId())
                .studentId(loan.getLibraryUser().getStudentId())
                .isbn(loan.getBook().getIsbn())
                .title(loan.getBook().getTitle())
                .author(loan.getBook().getAuthor())
                .status(loan.getStatus())
                .borrowedAt(loan.getBorrowedAt())
                .dueAt(loan.getDueAt())
                .returnedAt(loan.getReturnedAt())
                .build();
    }
}