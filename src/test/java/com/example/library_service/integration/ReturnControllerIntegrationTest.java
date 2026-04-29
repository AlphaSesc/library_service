package com.example.library_service.integration;

import com.example.library_service.client.FinanceClient;
import com.example.library_service.dto.ReturnResponse;
import com.example.library_service.dto.finance.CreateInvoiceRequest;
import com.example.library_service.dto.finance.InvoiceType;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Integration test for ReturnController covering book return operations.
// FinanceClient is MOCKED because the Finance microservice is not running during tests.
class ReturnControllerIntegrationTest {


    // Mock FinanceClient so the test doesn't try to reach the real Finance service.
    // @Primary ensures this mock overrides the real bean during this test.

    @TestConfiguration
    static class MockedFinanceClientConfig {
        @Bean
        @Primary
        public FinanceClient financeClient() {
            return mock(FinanceClient.class);
        }
    }


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

    @Autowired
    private FinanceClient financeClient;   // mock injected from TestConfiguration


    // Shared test state

    private RestClient restClient;
    private LibraryUser studentUser;
    private String studentToken;
    private String adminToken;
    private Book book;


    // Setup & Teardown


    @BeforeEach
    void setUp() {
        // Reset the mock so previous test interactions don't leak in
        reset(financeClient);

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

        // ---- Persist an active book with 4 available copies (1 already borrowed) ----
        book = Book.builder()
                .isbn("ISBN-001")
                .title("Effective Java")
                .author("Joshua Bloch")
                .totalCopies(5)
                .availableCopies(4)
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


    // Helpers: persists a Loan with controlled borrow/due times

    private Loan saveActiveLoan(LibraryUser user, Book book) {
        // Active loan with future due date (NOT overdue)
        Loan loan = Loan.builder()
                .libraryUser(user)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(2))
                .dueAt(LocalDateTime.now().plusDays(12))
                .returnedAt(null)
                .build();
        return loanRepository.save(loan);
    }

    private Loan saveOverdueLoan(LibraryUser user, Book book) {
        // Overdue loan – due date is in the past
        Loan loan = Loan.builder()
                .libraryUser(user)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(20))
                .dueAt(LocalDateTime.now().minusDays(2))
                .returnedAt(null)
                .build();
        return loanRepository.save(loan);
    }


    // Test 1 – POST /api/library/returns  →  successful on-time return

    @Test
    void shouldReturnBookSuccessfullyOnTime() {
        // Given – student has an active (not overdue) loan
        saveActiveLoan(studentUser, book);

        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – authenticated student sends POST request
        ResponseEntity<ReturnResponse> response = restClient.post()
                .uri("/api/library/returns")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(ReturnResponse.class);

        // Then – response is 200 OK with return details
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-LIB-001");
        assertThat(response.getBody().getIsbn()).isEqualTo("ISBN-001");
        assertThat(response.getBody().getTitle()).isEqualTo("Effective Java");
        assertThat(response.getBody().getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(response.getBody().getReturnedAt()).isNotNull();

        // Verify available copies increased from 4 to 5
        Book updatedBook = bookRepository.findByIsbn("ISBN-001").orElseThrow();
        assertThat(updatedBook.getAvailableCopies()).isEqualTo(5);

        // Verify NO invoice was created (return was on time)
        verify(financeClient, never()).createInvoice(any());
    }


    // Test 2 – POST /api/library/returns  →  late return generates fine invoice

    @Test
    void shouldReturnBookLateAndGenerateFineInvoice() {
        // Given – student has an OVERDUE loan
        saveOverdueLoan(studentUser, book);

        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – authenticated student sends POST request
        ResponseEntity<ReturnResponse> response = restClient.post()
                .uri("/api/library/returns")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(ReturnResponse.class);

        // Then – response is 200 OK
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(response.getBody().getReturnedAt()).isNotNull();

        // Verify Finance service was called with a LIBRARY_FINE invoice
        ArgumentCaptor<CreateInvoiceRequest> captor = ArgumentCaptor.forClass(CreateInvoiceRequest.class);
        verify(financeClient, times(1)).createInvoice(captor.capture());

        CreateInvoiceRequest sentRequest = captor.getValue();
        assertThat(sentRequest.getStudentId()).isEqualTo("STU-LIB-001");
        assertThat(sentRequest.getInvoiceType()).isEqualTo(InvoiceType.LIBRARY_FINE);
        assertThat(sentRequest.getAmount()).isEqualByComparingTo("500.0");
        assertThat(sentRequest.getCourseCode()).isNull();

        // Verify available copies still increased (return still happens)
        Book updatedBook = bookRepository.findByIsbn("ISBN-001").orElseThrow();
        assertThat(updatedBook.getAvailableCopies()).isEqualTo(5);
    }


    // Test 3 – POST /api/library/returns  →  fails when book ISBN doesn't exist

    @Test
    void shouldFailReturnWhenBookNotFound() {
        // Given – payload with non-existent ISBN
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-DOES-NOT-EXIST");

        // When – authenticated student sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/returns")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceNotFoundException: "Book not found")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify Finance service was NOT called
        verify(financeClient, never()).createInvoice(any());
    }


    // Test 4 – POST /api/library/returns  →  fails when no active loan exists

    @Test
    void shouldFailReturnWhenNoActiveLoanExists() {
        // Given – book exists but no loan for this student

        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – authenticated student tries to return without borrowing
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/returns")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceNotFoundException: "Active loan not found...")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify book copies unchanged
        Book unchangedBook = bookRepository.findByIsbn("ISBN-001").orElseThrow();
        assertThat(unchangedBook.getAvailableCopies()).isEqualTo(4);

        // Verify Finance service was NOT called
        verify(financeClient, never()).createInvoice(any());
    }


    // Test 5 – POST /api/library/returns  →  fails on already-returned loan
    //          (loan exists but returnedAt is set, so no active loan found)

    @Test
    void shouldFailReturnWhenLoanAlreadyReturned() {
        // Given – a loan that's already been returned
        Loan loan = Loan.builder()
                .libraryUser(studentUser)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(10))
                .dueAt(LocalDateTime.now().minusDays(0))
                .returnedAt(LocalDateTime.now().minusDays(1))
                .build();
        loanRepository.save(loan);

        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – authenticated student tries to return again
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/returns")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (no active loan found)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }


    // Test 6 – POST /api/library/returns  →  validation fails when ISBN missing

    @Test
    void shouldFailReturnWhenIsbnMissing() {
        // Given – payload without isbn
        Map<String, Object> request = new HashMap<>();

        // When – authenticated student sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/returns")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Bean Validation rejects with 400 Bad Request
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }


    // Test 7 – POST /api/library/returns  →  rejects ADMIN role

    @Test
    void shouldRejectAdminFromReturning() {
        // Given – an active loan exists (created for student, but admin will attempt)
        saveActiveLoan(studentUser, book);

        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – ADMIN tries to return a book
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/returns")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UnauthorizedOperationException from getCurrentStudentUser)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify Finance service was NOT called
        verify(financeClient, never()).createInvoice(any());
    }


    // Test 8 – POST /api/library/returns  →  401 without JWT token

    @Test
    void shouldReturn401WhenNoTokenProvided() {
        // Given – payload without Authorization header
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – unauthenticated POST is sent
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/returns")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }


    // Test 9 – Borrow then return flow updates loan and inventory correctly

    @Test
    void shouldUpdateLoanAndInventoryOnReturn() {
        // Given – an active loan exists
        Loan loan = saveActiveLoan(studentUser, book);

        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "ISBN-001");

        // When – authenticated student returns the book
        ResponseEntity<ReturnResponse> response = restClient.post()
                .uri("/api/library/returns")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(ReturnResponse.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);

        // Then – verify loan in DB now has returnedAt set
        Loan updatedLoan = loanRepository.findById(loan.getId()).orElseThrow();
        assertThat(updatedLoan.getReturnedAt()).isNotNull();

        // Verify inventory increased
        Book updatedBook = bookRepository.findByIsbn("ISBN-001").orElseThrow();
        assertThat(updatedBook.getAvailableCopies()).isEqualTo(5);
    }
}