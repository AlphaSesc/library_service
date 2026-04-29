package com.example.library_service.integration;

import com.example.library_service.dto.BookResponse;
import com.example.library_service.entity.Book;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.repository.BookRepository;
import com.example.library_service.repository.LibraryUserRepository;
import com.example.library_service.security.CustomLibraryUserDetails;
import com.example.library_service.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Integration test for student-facing BookController covering book listing
class BookControllerIntegrationTest {


    // Testcontainers: real MySQL container as backend database

    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0");


    // Random port assigned to embedded server

    @LocalServerPort
    private int port;


    // Spring-managed dependencies

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LibraryUserRepository libraryUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;


    // Shared test state

    private RestClient restClient;
    private String studentToken;


    // Setup & Teardown


    @BeforeEach
    void setUp() {
        // Build RestClient pointing at the embedded server
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { /* don't throw on errors */ })
                .build();

        // ---- Persist a STUDENT library user and generate JWT ----
        LibraryUser studentUser = LibraryUser.builder()
                .studentId("STU-LIB-001")
                .pinHash(passwordEncoder.encode("123456"))
                .role(LibraryRole.STUDENT)
                .firstLogin(false)
                .active(true)
                .build();
        libraryUserRepository.save(studentUser);
        studentToken = jwtService.generateToken(new CustomLibraryUserDetails(studentUser));
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test to keep them independent
        bookRepository.deleteAll();
        libraryUserRepository.deleteAll();
    }


    // Helper: persists a Book with the given values

    private Book saveBook(String isbn, String title, String author, int copies, boolean active) {
        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .author(author)
                .totalCopies(copies)
                .availableCopies(copies)
                .active(active)
                .build();
        return bookRepository.save(book);
    }


    // Test 1 – GET /api/library/books  →  returns all active books

    @Test
    void shouldGetAllActiveBooks() {
        // Given – two active books and one inactive book in the database
        saveBook("978-0-13-468599-1", "Effective Java", "Joshua Bloch", 5, true);
        saveBook("978-0-13-235088-4", "Clean Code", "Robert C. Martin", 3, true);

        // Save book then update it to inactive (bypasses @PrePersist's active=true default)
        Book inactive = saveBook("978-0-201-63361-0", "Old Inactive Book", "Some Author", 2, false);
        inactive.setActive(false);
        bookRepository.save(inactive);

        // When – authenticated student requests book list
        ResponseEntity<BookResponse[]> response = restClient.get()
                .uri("/api/library/books")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(BookResponse[].class);

        // Then – response is 200 OK with only the 2 active books
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);

        // Verify the inactive book is NOT in the response
        assertThat(response.getBody())
                .extracting(BookResponse::getTitle)
                .containsExactlyInAnyOrder("Effective Java", "Clean Code")
                .doesNotContain("Old Inactive Book");
    }


    // Test 2 – GET /api/library/books  →  returns empty list when no books

    @Test
    void shouldReturnEmptyListWhenNoBooksExist() {
        // Given – no books in the database

        // When – authenticated student requests book list
        ResponseEntity<BookResponse[]> response = restClient.get()
                .uri("/api/library/books")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(BookResponse[].class);

        // Then – response is 200 OK with empty array
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }


    // Test 3 – GET /api/library/books  →  returns book details correctly mapped

    @Test
    void shouldReturnBookDetailsCorrectly() {
        // Given – a single book in the database
        Book saved = saveBook("978-0-13-468599-1", "Effective Java", "Joshua Bloch", 5, true);

        // When – authenticated student requests book list
        ResponseEntity<BookResponse[]> response = restClient.get()
                .uri("/api/library/books")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(BookResponse[].class);

        // Then – response contains the book with correct field mapping
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(1);

        BookResponse book = response.getBody()[0];
        assertThat(book.getId()).isEqualTo(saved.getId());
        assertThat(book.getIsbn()).isEqualTo("978-0-13-468599-1");
        assertThat(book.getTitle()).isEqualTo("Effective Java");
        assertThat(book.getAuthor()).isEqualTo("Joshua Bloch");
        assertThat(book.getTotalCopies()).isEqualTo(5);
        assertThat(book.getAvailableCopies()).isEqualTo(5);
        assertThat(book.isActive()).isTrue();
    }

    @Test
    void shouldReturn401WhenAccessingBooksWithoutToken() {
        // Given – a book exists
        saveBook("978-0-13-468599-1", "Effective Java", "Joshua Bloch", 5, true);

        // When – unauthenticated GET is sent (no Authorization header)
        ResponseEntity<String> response = restClient.get()
                .uri("/api/library/books")
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}