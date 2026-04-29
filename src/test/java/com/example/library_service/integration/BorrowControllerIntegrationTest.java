package com.example.library_service.integration;

import com.example.library_service.dto.BorrowResponse;
import com.example.library_service.entity.Book;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.entity.Loan;
import com.example.library_service.entity.LoanStatus;
import com.example.library_service.repository.BookRepository;
import com.example.library_service.repository.LibraryUserRepository;
import com.example.library_service.repository.LoanRepository;
import com.example.library_service.security.CustomLibraryUserDetails;
import com.example.library_service.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Integration test for BorrowController covering book borrowing operations
class BorrowControllerIntegrationTest {


    // Testcontainers: real MySQL container as backend database

    @Container
    @ServiceConnection
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0");


    // Random port assigned to embedded server

    @LocalServerPort
    private int port;


    // Spring-managed dependencies

    @Autowired
    private LibraryUserRepository libraryUserRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;


    // Shared test state

    private RestClient restClient;
    private LibraryUser studentUser;
    private String studentToken;
    private String adminToken;
    private Book book;


    // Setup & Teardown


    @BeforeEach
    void setUp() {
        // Build RestClient pointing at the embedded server
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { /* don't throw on errors */ })
                .build();

        // ---- Persist a STUDENT library user and generate JWT ----
        studentUser = LibraryUser.builder()
                .studentId("STU-LIB-001")
                .pinHash(passwordEncoder.encode("123456"))
                .role(LibraryRole.STUDENT)
                .firstLogin(false)
                .active(true)
                .build();
        studentUser = libraryUserRepository.save(studentUser);
        studentToken = jwtService.generateToken(new CustomLibraryUserDetails(studentUser));

        // ---- Persist an ADMIN user (used to verify role rejection) ----
        LibraryUser adminUser = LibraryUser.builder()
                .studentId("ADMIN-001")
                .pinHash(passwordEncoder.encode("admin123"))
                .role(LibraryRole.ADMIN)
                .firstLogin(false)
                .active(true)
                .build();
        libraryUserRepository.save(adminUser);
        adminToken = jwtService.generateToken(new CustomLibraryUserDetails(adminUser));

        // ---- Persist an active book with 5 available copies ----
        book = Book.builder()
                .isbn("ISBN-001")
                .title("Effective Java")
                .author("Joshua Bloch")
                .totalCopies(5)
                .availableCopies(5)
                .active(true)
                .build();
        book = bookRepository.save(book);
    }

    @AfterEach
    void tearDown() {
        // Clear in dependency-safe order: loans → books → users
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        libraryUserRepository.deleteAll();
    }


    // Test 1 – POST /api/library/borrow  →  successfully borrow a book

    @Test
    void shouldBorrowBookSuccessfully() {
        // Given – payload with the persisted book's ISBN
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – authenticated student sends POST request
        ResponseEntity<BorrowResponse> response = restClient.post()
                .uri("/api/library/borrow")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(BorrowResponse.class);

        // Then – response is 200 OK with loan details
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getLoanId()).isNotNull();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-LIB-001");
        assertThat(response.getBody().getIsbn()).isEqualTo("ISBN-001");
        assertThat(response.getBody().getTitle()).isEqualTo("Effective Java");
        assertThat(response.getBody().getStatus()).isEqualTo(LoanStatus.BORROWED);
        assertThat(response.getBody().getBorrowedAt()).isNotNull();
        assertThat(response.getBody().getDueAt()).isNotNull();

        // Verify loan was persisted
        assertThat(loanRepository.findAll()).hasSize(1);

        // Verify book's available copies decreased from 5 to 4
        Book updatedBook = bookRepository.findByIsbn("ISBN-001").orElseThrow();
        assertThat(updatedBook.getAvailableCopies()).isEqualTo(4);
        assertThat(updatedBook.getTotalCopies()).isEqualTo(5);  // total stays the same
    }


    // Test 2 – POST /api/library/borrow  →  fails when book ISBN doesn't exist

    @Test
    void shouldFailBorrowWhenBookNotFound() {
        // Given – payload with a non-existent ISBN
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-DOES-NOT-EXIST");

        // When – authenticated student sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/borrow")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceNotFoundException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify no loan was created
        assertThat(loanRepository.findAll()).isEmpty();
    }


    // Test 3 – POST /api/library/borrow  →  fails when book is inactive

    @Test
    void shouldFailBorrowWhenBookInactive() {
        // Given – mark the book as inactive
        book.setActive(false);
        bookRepository.save(book);

        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – authenticated student sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/borrow")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceNotFoundException: "Book not found")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify no loan was created
        assertThat(loanRepository.findAll()).isEmpty();
    }


    // Test 4 – POST /api/library/borrow  →  fails when no copies available

    @Test
    void shouldFailBorrowWhenNoCopiesAvailable() {
        // Given – set available copies to 0
        book.setAvailableCopies(0);
        bookRepository.save(book);

        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – authenticated student sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/borrow")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (NoAvailableCopiesException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify available copies still 0 (unchanged)
        Book unchangedBook = bookRepository.findByIsbn("ISBN-001").orElseThrow();
        assertThat(unchangedBook.getAvailableCopies()).isEqualTo(0);
    }


    // Test 5 – POST /api/library/borrow  →  fails on duplicate borrow (same book)

    @Test
    void shouldFailWhenAlreadyBorrowedSameBook() {
        // Given – student already has an active loan for this book
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // First borrow succeeds
        restClient.post()
                .uri("/api/library/borrow")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(BorrowResponse.class);

        // When – student tries to borrow the same book again
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/borrow")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceAlreadyExistsException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify only ONE loan exists
        assertThat(loanRepository.findAll()).hasSize(1);

        // Verify book copies decreased only once (from 5 to 4)
        Book updatedBook = bookRepository.findByIsbn("ISBN-001").orElseThrow();
        assertThat(updatedBook.getAvailableCopies()).isEqualTo(4);
    }


    // Test 6 – POST /api/library/borrow  →  validation fails when ISBN missing

    @Test
    void shouldFailBorrowWhenIsbnMissing() {
        // Given – payload without isbn field
        Map<String, Object> request = new HashMap<>();

        // When – authenticated student sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/borrow")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Bean Validation rejects with 400 Bad Request
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }


    // Test 7 – POST /api/library/borrow  →  rejects ADMIN role

    @Test
    void shouldRejectAdminFromBorrowing() {
        // Given – payload with valid ISBN
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – ADMIN tries to borrow a book
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/borrow")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UnauthorizedOperationException from getCurrentStudentUser)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify no loan was created
        assertThat(loanRepository.findAll()).isEmpty();
    }


    // Test 8 – Multiple borrows of different books decrement inventory correctly

    @Test
    void shouldAllowBorrowingDifferentBooks() {
        // Given – another active book in the database
        Book secondBook = Book.builder()
                .isbn("ISBN-002")
                .title("Clean Code")
                .author("Robert C. Martin")
                .totalCopies(3)
                .availableCopies(3)
                .active(true)
                .build();
        bookRepository.save(secondBook);

        // ---- Borrow first book ----
        Map<String, Object> request1 = new HashMap<>();
        request1.put("isbn", "ISBN-001");
        ResponseEntity<BorrowResponse> response1 = restClient.post()
                .uri("/api/library/borrow")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request1)
                .retrieve()
                .toEntity(BorrowResponse.class);
        assertThat(response1.getStatusCode().value()).isEqualTo(200);

        // ---- Borrow second book ----
        Map<String, Object> request2 = new HashMap<>();
        request2.put("isbn", "ISBN-002");
        ResponseEntity<BorrowResponse> response2 = restClient.post()
                .uri("/api/library/borrow")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request2)
                .retrieve()
                .toEntity(BorrowResponse.class);
        assertThat(response2.getStatusCode().value()).isEqualTo(200);

        // Verify both loans exist and inventory updated correctly
        assertThat(loanRepository.findAll()).hasSize(2);
        assertThat(bookRepository.findByIsbn("ISBN-001").orElseThrow().getAvailableCopies()).isEqualTo(4);
        assertThat(bookRepository.findByIsbn("ISBN-002").orElseThrow().getAvailableCopies()).isEqualTo(2);
    }


    // Test 9 – POST /api/library/borrow  →  401 without JWT token

    @Test
    void shouldReturn401WhenNoTokenProvided() {
        // Given – payload without Authorization header
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – unauthenticated POST is sent
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/borrow")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}