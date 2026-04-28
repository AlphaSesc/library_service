package com.example.library_service.service;

import com.example.library_service.dto.LoanHistoryResponse;
import com.example.library_service.dto.StudentLoanSummaryResponse;
import com.example.library_service.entity.Book;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LoanStatus;
import com.example.library_service.repository.LibraryUserRepository;
import com.example.library_service.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminLoanServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LibraryUserRepository libraryUserRepository;

    @Mock
    private AuthenticatedLibraryUserService authenticatedLibraryUserService;

    @InjectMocks
    private AdminLoanService adminLoanService;

    private LibraryUser adminUser;
    private LibraryUser studentUser;
    private Book book;
    private Loan activeLoan;
    private Loan overdueLoan;

    @BeforeEach
    void setUp() {

        adminUser = LibraryUser.builder()
                .id(1L)
                .studentId("ADMIN-1")
                .role(LibraryRole.ADMIN)
                .active(true)
                .build();

        studentUser = LibraryUser.builder()
                .id(2L)
                .studentId("STU-100")
                .role(LibraryRole.STUDENT)
                .active(true)
                .build();

        book = Book.builder()
                .id(1L)
                .isbn("978-3-16-148410-0")
                .title("Distributed Systems")
                .author("Ujjwal")
                .totalCopies(5)
                .availableCopies(3)
                .active(true)
                .build();

        activeLoan = Loan.builder()
                .id(1L)
                .libraryUser(studentUser)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(2))
                .dueAt(LocalDateTime.now().plusDays(5))
                .returnedAt(null)
                .build();

        overdueLoan = Loan.builder()
                .id(2L)
                .libraryUser(studentUser)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(10))
                .dueAt(LocalDateTime.now().minusDays(2))
                .returnedAt(null)
                .build();
    }

    @Test
    void getCurrentLoansShouldReturnAllActiveLoans() {

        when(authenticatedLibraryUserService.getCurrentAdminUser())
                .thenReturn(adminUser);

        when(loanRepository.findByReturnedAtIsNullOrderByBorrowedAtDesc())
                .thenReturn(List.of(activeLoan));

        List<LoanHistoryResponse> responses =
                adminLoanService.getCurrentLoans();

        assertEquals(1, responses.size());

        LoanHistoryResponse response = responses.getFirst();

        assertEquals(activeLoan.getId(), response.getLoanId());
        assertEquals("STU-100", response.getStudentId());
        assertEquals("978-3-16-148410-0", response.getIsbn());
        assertEquals("Distributed Systems", response.getTitle());
        assertEquals("Ujjwal", response.getAuthor());
        assertEquals(LoanStatus.BORROWED, response.getStatus());

        verify(loanRepository)
                .findByReturnedAtIsNullOrderByBorrowedAtDesc();
    }

    @Test
    void getCurrentLoansShouldReturnEmptyListWhenNoLoansExist() {

        when(authenticatedLibraryUserService.getCurrentAdminUser())
                .thenReturn(adminUser);

        when(loanRepository.findByReturnedAtIsNullOrderByBorrowedAtDesc())
                .thenReturn(List.of());

        List<LoanHistoryResponse> responses =
                adminLoanService.getCurrentLoans();

        assertTrue(responses.isEmpty());
    }

    @Test
    void getOverdueLoansShouldReturnOnlyOverdueLoans() {

        when(authenticatedLibraryUserService.getCurrentAdminUser())
                .thenReturn(adminUser);

        when(loanRepository.findByReturnedAtIsNullAndDueAtBeforeOrderByBorrowedAtDesc(any()))
                .thenReturn(List.of(overdueLoan));

        List<LoanHistoryResponse> responses =
                adminLoanService.getOverdueLoans();

        assertEquals(1, responses.size());

        LoanHistoryResponse response = responses.getFirst();

        assertEquals(overdueLoan.getId(), response.getLoanId());
        assertEquals(LoanStatus.OVERDUE, response.getStatus());

        verify(loanRepository)
                .findByReturnedAtIsNullAndDueAtBeforeOrderByBorrowedAtDesc(any());
    }

    @Test
    void getOverdueLoansShouldReturnEmptyListWhenNoOverdueLoansExist() {

        when(authenticatedLibraryUserService.getCurrentAdminUser())
                .thenReturn(adminUser);

        when(loanRepository.findByReturnedAtIsNullAndDueAtBeforeOrderByBorrowedAtDesc(any()))
                .thenReturn(List.of());

        List<LoanHistoryResponse> responses =
                adminLoanService.getOverdueLoans();

        assertTrue(responses.isEmpty());
    }

    @Test
    void getStudentLoanSummariesShouldReturnLoanSummaryForStudents() {

        when(authenticatedLibraryUserService.getCurrentAdminUser())
                .thenReturn(adminUser);

        when(libraryUserRepository.findByRole(LibraryRole.STUDENT))
                .thenReturn(List.of(studentUser));

        when(loanRepository.countByLibraryUserAndReturnedAtIsNull(studentUser))
                .thenReturn(2L);

        when(loanRepository.countByLibraryUserAndReturnedAtIsNullAndDueAtBefore(
                eq(studentUser),
                any(LocalDateTime.class)
        )).thenReturn(1L);

        List<StudentLoanSummaryResponse> responses =
                adminLoanService.getStudentLoanSummaries();

        assertEquals(1, responses.size());

        StudentLoanSummaryResponse response = responses.getFirst();

        assertEquals("STU-100", response.getStudentId());
        assertEquals(2L, response.getBooksOnLoan());
        assertEquals(1L, response.getOverdueBooks());

        verify(libraryUserRepository)
                .findByRole(LibraryRole.STUDENT);
    }

    @Test
    void getStudentLoanSummariesShouldReturnEmptyListWhenNoStudentsExist() {

        when(authenticatedLibraryUserService.getCurrentAdminUser())
                .thenReturn(adminUser);

        when(libraryUserRepository.findByRole(LibraryRole.STUDENT))
                .thenReturn(List.of());

        List<StudentLoanSummaryResponse> responses =
                adminLoanService.getStudentLoanSummaries();

        assertTrue(responses.isEmpty());
    }

    @Test
    void getCurrentLoansShouldCorrectlyMapLoanFields() {

        when(authenticatedLibraryUserService.getCurrentAdminUser())
                .thenReturn(adminUser);

        when(loanRepository.findByReturnedAtIsNullOrderByBorrowedAtDesc())
                .thenReturn(List.of(activeLoan));

        LoanHistoryResponse response =
                adminLoanService.getCurrentLoans().getFirst();

        assertEquals(activeLoan.getId(), response.getLoanId());
        assertEquals(studentUser.getStudentId(), response.getStudentId());
        assertEquals(book.getIsbn(), response.getIsbn());
        assertEquals(book.getTitle(), response.getTitle());
        assertEquals(book.getAuthor(), response.getAuthor());
        assertEquals(activeLoan.getBorrowedAt(), response.getBorrowedAt());
        assertEquals(activeLoan.getDueAt(), response.getDueAt());
    }
}