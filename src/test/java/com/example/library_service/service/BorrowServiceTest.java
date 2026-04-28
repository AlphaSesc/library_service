package com.example.library_service.service;

import com.example.library_service.dto.BorrowRequest;
import com.example.library_service.dto.BorrowResponse;
import com.example.library_service.entity.Book;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LoanStatus;
import com.example.library_service.exception.InactiveUserException;
import com.example.library_service.exception.NoAvailableCopiesException;
import com.example.library_service.exception.ResourceAlreadyExistsException;
import com.example.library_service.exception.ResourceNotFoundException;
import com.example.library_service.repository.BookRepository;
import com.example.library_service.repository.LoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BorrowServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthenticatedLibraryUserService authenticatedLibraryUserService;

    @InjectMocks
    private BorrowService borrowService;

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
                .availableCopies(5)
                .active(true)
                .build();

        loan = Loan.builder()
                .id(1L)
                .libraryUser(libraryUser)
                .book(book)
                .borrowedAt(LocalDateTime.now())
                .dueAt(LocalDateTime.now().plusDays(14))
                .returnedAt(null)
                .build();
    }

    @Test
    void borrowBookShouldBorrowBookSuccessfully() {

        BorrowRequest request = new BorrowRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        when(loanRepository.findByLibraryUserAndBookAndReturnedAtIsNull(
                libraryUser,
                book
        )).thenReturn(Optional.empty());

        when(loanRepository.save(any(Loan.class)))
                .thenReturn(loan);

        BorrowResponse response = borrowService.borrowBook(request);

        assertNotNull(response);

        assertEquals("STU-100", response.getStudentId());
        assertEquals("978-3-16-148410-0", response.getIsbn());
        assertEquals("Distributed Systems", response.getTitle());
        assertEquals(LoanStatus.BORROWED, response.getStatus());

        assertEquals(4, book.getAvailableCopies());

        verify(loanRepository).save(any(Loan.class));
        verify(bookRepository).save(book);
    }

    @Test
    void borrowBookShouldThrowWhenLibraryUserIsInactive() {

        libraryUser.setActive(false);

        BorrowRequest request = new BorrowRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        InactiveUserException exception = assertThrows(
                InactiveUserException.class,
                () -> borrowService.borrowBook(request)
        );

        assertEquals(
                "Inactive library user cannot borrow books",
                exception.getMessage()
        );

        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrowBookShouldThrowWhenBookDoesNotExist() {

        BorrowRequest request = new BorrowRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> borrowService.borrowBook(request)
        );

        assertEquals(
                "Book not found",
                exception.getMessage()
        );
    }

    @Test
    void borrowBookShouldThrowWhenBookIsInactive() {

        book.setActive(false);

        BorrowRequest request = new BorrowRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> borrowService.borrowBook(request)
        );

        assertEquals(
                "Book not found",
                exception.getMessage()
        );
    }

    @Test
    void borrowBookShouldThrowWhenNoCopiesAreAvailable() {

        book.setAvailableCopies(0);

        BorrowRequest request = new BorrowRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        NoAvailableCopiesException exception = assertThrows(
                NoAvailableCopiesException.class,
                () -> borrowService.borrowBook(request)
        );

        assertEquals(
                "No available copies for this book",
                exception.getMessage()
        );
    }

    @Test
    void borrowBookShouldThrowWhenBookAlreadyBorrowedByUser() {

        BorrowRequest request = new BorrowRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        when(loanRepository.findByLibraryUserAndBookAndReturnedAtIsNull(
                libraryUser,
                book
        )).thenReturn(Optional.of(loan));

        ResourceAlreadyExistsException exception = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> borrowService.borrowBook(request)
        );

        assertEquals(
                "You have already borrowed this book",
                exception.getMessage()
        );

        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrowBookShouldDecreaseAvailableCopiesAfterBorrowing() {

        BorrowRequest request = new BorrowRequest();
        request.setIsbn("978-3-16-148410-0");

        when(authenticatedLibraryUserService.getCurrentStudentUser())
                .thenReturn(libraryUser);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        when(loanRepository.findByLibraryUserAndBookAndReturnedAtIsNull(
                libraryUser,
                book
        )).thenReturn(Optional.empty());

        when(loanRepository.save(any(Loan.class)))
                .thenReturn(loan);

        borrowService.borrowBook(request);

        assertEquals(4, book.getAvailableCopies());
    }
}