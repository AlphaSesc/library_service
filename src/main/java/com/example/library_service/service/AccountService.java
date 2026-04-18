package com.example.library_service.service;

import com.example.library_service.dto.LoanHistoryResponse;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.repository.LoanRepository;
import com.example.library_service.util.LoanStatusResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final LoanRepository loanRepository;
    private final AuthenticatedLibraryUserService authenticatedLibraryUserService;

    public List<LoanHistoryResponse> getMyBorrowingHistory() {
        LibraryUser libraryUser = authenticatedLibraryUserService.getCurrentStudentUser();

        return loanRepository.findByLibraryUserOrderByBorrowedAtDesc(libraryUser)
                .stream()
                .map(loan -> LoanHistoryResponse.builder()
                        .loanId(loan.getId())
                        .studentId(libraryUser.getStudentId())
                        .isbn(loan.getBook().getIsbn())
                        .title(loan.getBook().getTitle())
                        .author(loan.getBook().getAuthor())
                        .status(LoanStatusResolver.resolve(loan))
                        .borrowedAt(loan.getBorrowedAt())
                        .dueAt(loan.getDueAt())
                        .returnedAt(loan.getReturnedAt())
                        .build())
                .toList();
    }
}