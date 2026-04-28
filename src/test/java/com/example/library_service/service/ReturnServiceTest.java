package com.example.library_service.service;

import com.example.library_service.client.FinanceClient;
import com.example.library_service.dto.ReturnRequest;
import com.example.library_service.dto.ReturnResponse;
import com.example.library_service.entity.Book;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LoanStatus;
import com.example.library_service.exception.InactiveUserException;
import com.example.library_service.exception.ResourceNotFoundException;
import com.example.library_service.repository.BookRepository;
import com.example.library_service.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReturnServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthenticatedLibraryUserService authenticatedLibraryUserService;

    @Mock
    private FinanceClient financeClient;

    @InjectMocks
    private ReturnService returnService;

    private LibraryUser libraryUser;
    private Book book;
    private Loan loan;

    @BeforeEach
    void setUp() {

        libraryUser = LibraryUser.builder()
                .id(1L)
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
                .availableCopies(4)
                .active(true)
                .build();

        loan = Loan.builder()
                .id(1L)
                .libraryUser(libraryUser)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(10))
                .dueAt(LocalDateTime.now().plusDays(2))
                .returnedAt(null)
                .build();
    }

    @Test
    void returnBookShouldReturnBookSuccessfully() {

        ReturnRequest request = new ReturnRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        when(loanRepository.findByLibraryUserAndBookAndReturnedAtIsNull(
                libraryUser,
                book
        )).thenReturn(Optional.of(loan));

        when(loanRepository.save(any(Loan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnResponse response = returnService.returnBook(request);

        assertNotNull(response);

        assertEquals("STU-100", response.getStudentId());
        assertEquals("978-3-16-148410-0", response.getIsbn());
        assertEquals("Distributed Systems", response.getTitle());
        assertEquals(LoanStatus.RETURNED, response.getStatus());

        assertNotNull(response.getReturnedAt());

        assertEquals(5, book.getAvailableCopies());

        verify(bookRepository).save(book);
        verify(financeClient, never()).createInvoice(any());
    }

    @Test
    void returnBookShouldThrowWhenLibraryUserIsInactive() {

        libraryUser.setActive(false);

        ReturnRequest request = new ReturnRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        InactiveUserException exception = assertThrows(
                InactiveUserException.class,
                () -> returnService.returnBook(request)
        );

        assertEquals(
                "Inactive library user cannot return books",
                exception.getMessage()
        );

        verify(loanRepository, never()).save(any());
    }

    @Test
    void returnBookShouldThrowWhenBookDoesNotExist() {

        ReturnRequest request = new ReturnRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> returnService.returnBook(request)
        );

        assertEquals(
                "Book not found",
                exception.getMessage()
        );
    }

    @Test
    void returnBookShouldThrowWhenActiveLoanDoesNotExist() {

        ReturnRequest request = new ReturnRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        when(loanRepository.findByLibraryUserAndBookAndReturnedAtIsNull(
                libraryUser,
                book
        )).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> returnService.returnBook(request)
        );

        assertEquals(
                "Active loan not found for this book",
                exception.getMessage()
        );
    }

    @Test
    void returnBookShouldIncreaseAvailableCopiesAfterReturn() {

        ReturnRequest request = new ReturnRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        when(loanRepository.findByLibraryUserAndBookAndReturnedAtIsNull(
                libraryUser,
                book
        )).thenReturn(Optional.of(loan));

        when(loanRepository.save(any(Loan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        returnService.returnBook(request);

        assertEquals(5, book.getAvailableCopies());

        verify(bookRepository).save(book);
    }

    @Test
    void returnBookShouldCreateFineInvoiceWhenBookIsReturnedLate() {

        loan.setDueAt(LocalDateTime.now().minusDays(2));

        ReturnRequest request = new ReturnRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        when(loanRepository.findByLibraryUserAndBookAndReturnedAtIsNull(
                libraryUser,
                book
        )).thenReturn(Optional.of(loan));

        when(loanRepository.save(any(Loan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        returnService.returnBook(request);

        verify(financeClient).createInvoice(
                ArgumentMatchers.any()
        );
    }

    @Test
    void returnBookShouldNotCreateFineInvoiceWhenReturnedOnTime() {

        loan.setDueAt(LocalDateTime.now().plusDays(2));

        ReturnRequest request = new ReturnRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        when(loanRepository.findByLibraryUserAndBookAndReturnedAtIsNull(
                libraryUser,
                book
        )).thenReturn(Optional.of(loan));

        when(loanRepository.save(any(Loan.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        returnService.returnBook(request);

        verify(financeClient, never()).createInvoice(any());
    }
}