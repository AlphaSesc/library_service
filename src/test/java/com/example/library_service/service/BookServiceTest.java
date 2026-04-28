package com.example.library_service.service;

import com.example.library_service.dto.AddBookRequest;
import com.example.library_service.dto.BookResponse;
import com.example.library_service.entity.Book;
import com.example.library_service.exception.ResourceAlreadyExistsException;
import com.example.library_service.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    private Book book;

    @BeforeEach
    void setUp() {

        book = Book.builder()
                .id(1L)
                .isbn("978-3-16-148410-0")
                .title("Distributed Systems")
                .author("Ujjwal")
                .totalCopies(5)
                .availableCopies(5)
                .active(true)
                .build();
    }

    @Test
    void getAllBooksShouldReturnActiveBooks() {

        Book secondBook = Book.builder()
                .id(2L)
                .isbn("978-0-12-345678-9")
                .title("Microservices")
                .author("Martin")
                .totalCopies(3)
                .availableCopies(2)
                .active(true)
                .build();

        when(bookRepository.findByActiveTrue())
                .thenReturn(List.of(book, secondBook));

        List<BookResponse> responses = bookService.getAllBooks();

        assertEquals(2, responses.size());

        BookResponse firstResponse = responses.getFirst();

        assertEquals("978-3-16-148410-0", firstResponse.getIsbn());
        assertEquals("Distributed Systems", firstResponse.getTitle());
        assertEquals("Ujjwal", firstResponse.getAuthor());
        assertEquals(5, firstResponse.getTotalCopies());
        assertEquals(5, firstResponse.getAvailableCopies());
        assertTrue(firstResponse.isActive());

        verify(bookRepository).findByActiveTrue();
    }

    @Test
    void getAllBooksShouldReturnEmptyListWhenNoBooksExist() {

        when(bookRepository.findByActiveTrue())
                .thenReturn(List.of());

        List<BookResponse> responses = bookService.getAllBooks();

        assertTrue(responses.isEmpty());
    }

    @Test
    void addBookShouldCreateBookSuccessfully() {

        AddBookRequest request = new AddBookRequest();
        request.setIsbn("978-3-16-148410-0");
        request.setTitle("Distributed Systems");
        request.setAuthor("Ujjwal");
        request.setTotalCopies(5);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.empty());

        when(bookRepository.save(any(Book.class)))
                .thenReturn(book);

        BookResponse response = bookService.addBook(request);

        assertNotNull(response);

        assertEquals("978-3-16-148410-0", response.getIsbn());
        assertEquals("Distributed Systems", response.getTitle());
        assertEquals("Ujjwal", response.getAuthor());
        assertEquals(5, response.getTotalCopies());
        assertEquals(5, response.getAvailableCopies());

        verify(bookRepository).save(any(Book.class));
    }

    @Test
    void addBookShouldThrowWhenBookWithSameIsbnAlreadyExists() {

        AddBookRequest request = new AddBookRequest();
        request.setIsbn("978-3-16-148410-0");
        request.setTitle("Distributed Systems");
        request.setAuthor("Ujjwal");
        request.setTotalCopies(5);

        when(bookRepository.findByIsbn("978-3-16-148410-0"))
                .thenReturn(Optional.of(book));

        ResourceAlreadyExistsException exception = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> bookService.addBook(request)
        );

        assertEquals(
                "Book with this ISBN already exists",
                exception.getMessage()
        );

        verify(bookRepository, never()).save(any());
    }

    @Test
    void addBookShouldInitializeAvailableCopiesEqualToTotalCopies() {

        AddBookRequest request = new AddBookRequest();
        request.setIsbn("978-0-12-345678-9");
        request.setTitle("Clean Architecture");
        request.setAuthor("Robert Martin");
        request.setTotalCopies(10);

        Book savedBook = Book.builder()
                .id(2L)
                .isbn("978-0-12-345678-9")
                .title("Clean Architecture")
                .author("Robert Martin")
                .totalCopies(10)
                .availableCopies(10)
                .active(true)
                .build();

        when(bookRepository.findByIsbn("978-0-12-345678-9"))
                .thenReturn(Optional.empty());

        when(bookRepository.save(any(Book.class)))
                .thenReturn(savedBook);

        BookResponse response = bookService.addBook(request);

        assertEquals(10, response.getTotalCopies());
        assertEquals(10, response.getAvailableCopies());
    }
}