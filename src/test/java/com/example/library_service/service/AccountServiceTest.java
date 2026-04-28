package com.example.library_service.service;

import com.example.library_service.dto.LoanHistoryResponse;
import com.example.library_service.entity.Book;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LoanStatus;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private AuthenticatedLibraryUserService authenticatedLibraryUserService;

    @InjectMocks
    private AccountService accountService;

    private LibraryUser libraryUser;
    private Book book;
    private Loan activeLoan;
    private Loan returnedLoan;

    @BeforeEach
    void setUp() {

        libraryUser = LibraryUser.builder()
                .id(1L)
                .studentId("STU-100")
                .build();

        book = Book.builder()
                .id(1L)
                .isbn("978-3-16-148410-0")
                .title("Distributed Systems")
                .author("Ujjwal")
                .build();

        activeLoan = Loan.builder()
                .id(1L)
                .libraryUser(libraryUser)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(5))
                .dueAt(LocalDateTime.now().plusDays(5))
                .returnedAt(null)
                .build();

        returnedLoan = Loan.builder()
                .id(2L)
                .libraryUser(libraryUser)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(10))
                .dueAt(LocalDateTime.now().minusDays(3))
                .returnedAt(LocalDateTime.now().minusDays(2))
                .build();
    }

    @Test
    void getMyBorrowingsShouldReturnActiveBorrowings() {

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(loanRepository.findByLibraryUserAndReturnedAtIsNullOrderByBorrowedAtDesc(libraryUser))
                .thenReturn(List.of(activeLoan));

        List<LoanHistoryResponse> responses = accountService.getMyBorrowings();

        assertEquals(1, responses.size());

        LoanHistoryResponse response = responses.getFirst();

        assertEquals(activeLoan.getId(), response.getLoanId());
        assertEquals("STU-100", response.getStudentId());
        assertEquals("978-3-16-148410-0", response.getIsbn());
        assertEquals("Distributed Systems", response.getTitle());
        assertEquals("Ujjwal", response.getAuthor());
        assertEquals(LoanStatus.BORROWED, response.getStatus());

        verify(loanRepository)
                .findByLibraryUserAndReturnedAtIsNullOrderByBorrowedAtDesc(libraryUser);
    }

    @Test
    void getMyBorrowingsShouldReturnEmptyListWhenNoActiveBorrowings() {

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(loanRepository.findByLibraryUserAndReturnedAtIsNullOrderByBorrowedAtDesc(libraryUser))
                .thenReturn(List.of());

        List<LoanHistoryResponse> responses = accountService.getMyBorrowings();

        assertTrue(responses.isEmpty());
    }

    @Test
    void getMyBorrowingHistoryShouldReturnFullBorrowingHistory() {

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(loanRepository.findByLibraryUserOrderByBorrowedAtDesc(libraryUser))
                .thenReturn(List.of(activeLoan, returnedLoan));

        List<LoanHistoryResponse> responses =
                accountService.getMyBorrowingHistory();

        assertEquals(2, responses.size());

        LoanHistoryResponse activeResponse = responses.get(0);
        LoanHistoryResponse returnedResponse = responses.get(1);

        assertEquals(LoanStatus.BORROWED, activeResponse.getStatus());
        assertEquals(LoanStatus.RETURNED, returnedResponse.getStatus());

        assertEquals("Distributed Systems", activeResponse.getTitle());
        assertEquals("Distributed Systems", returnedResponse.getTitle());

        verify(loanRepository)
                .findByLibraryUserOrderByBorrowedAtDesc(libraryUser);
    }

    @Test
    void getMyBorrowingHistoryShouldReturnEmptyListWhenNoHistoryExists() {

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(loanRepository.findByLibraryUserOrderByBorrowedAtDesc(libraryUser))
                .thenReturn(List.of());

        List<LoanHistoryResponse> responses =
                accountService.getMyBorrowingHistory();

        assertTrue(responses.isEmpty());
    }

    @Test
    void getMyBorrowingHistoryShouldCorrectlyMapReturnedLoanFields() {

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(loanRepository.findByLibraryUserOrderByBorrowedAtDesc(libraryUser))
                .thenReturn(List.of(returnedLoan));

        LoanHistoryResponse response =
                accountService.getMyBorrowingHistory().getFirst();

        assertEquals(returnedLoan.getId(), response.getLoanId());
        assertEquals("STU-100", response.getStudentId());
        assertEquals(book.getIsbn(), response.getIsbn());
        assertEquals(book.getTitle(), response.getTitle());
        assertEquals(book.getAuthor(), response.getAuthor());
        assertEquals(LoanStatus.RETURNED, response.getStatus());

        assertNotNull(response.getReturnedAt());
    }
}