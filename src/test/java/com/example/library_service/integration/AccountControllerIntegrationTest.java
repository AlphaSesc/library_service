package com.example.library_service.integration;

import com.example.library_service.dto.LoanHistoryResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// Integration test for AccountController covering current borrowings and borrowing history
class AccountControllerIntegrationTest {


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
    private LibraryUser otherStudent;
    private String studentToken;
    private String otherStudentToken;
    private String adminToken;


    // Setup & Teardown


    @BeforeEach
    void setUp() {
        // Build RestClient pointing at the embedded server
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { /* don't throw on errors */ })
                .build();

        // ---- Persist a STUDENT library user ----
        studentUser = LibraryUser.builder()
                .studentId("STU-LIB-001")
                .pinHash(passwordEncoder.encode("123456"))
                .role(LibraryRole.STUDENT)
                .firstLogin(false)
                .active(true)
                .build();
        studentUser = libraryUserRepository.save(studentUser);
        studentToken = jwtService.generateToken(new CustomLibraryUserDetails(studentUser));

        // ---- Persist another STUDENT (used to verify isolation between students) ----
        otherStudent = LibraryUser.builder()
                .studentId("STU-LIB-002")
                .pinHash(passwordEncoder.encode("123456"))
                .role(LibraryRole.STUDENT)
                .firstLogin(false)
                .active(true)
                .build();
        otherStudent = libraryUserRepository.save(otherStudent);
        otherStudentToken = jwtService.generateToken(new CustomLibraryUserDetails(otherStudent));

        // ---- Persist an ADMIN (used to verify role rejection) ----
        LibraryUser adminUser = LibraryUser.builder()
                .studentId("ADMIN-001")
                .pinHash(passwordEncoder.encode("admin123"))
                .role(LibraryRole.ADMIN)
                .firstLogin(false)
                .active(true)
                .build();
        libraryUserRepository.save(adminUser);
        adminToken = jwtService.generateToken(new CustomLibraryUserDetails(adminUser));
    }

    @AfterEach
    void tearDown() {
        // Clear in dependency-safe order: loans → books → users
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        libraryUserRepository.deleteAll();
    }


    // Helpers: create persisted Book and Loan entities

    private Book saveBook(String isbn, String title, String author) {
        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .author(author)
                .totalCopies(5)
                .availableCopies(5)
                .active(true)
                .build();
        return bookRepository.save(book);
    }

    // Persists an active loan (not yet returned)
    private Loan saveActiveLoan(LibraryUser user, Book book) {
        Loan loan = Loan.builder()
                .libraryUser(user)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(2))
                .dueAt(LocalDateTime.now().plusDays(12))
                .returnedAt(null)
                .build();
        return loanRepository.save(loan);
    }

    // Persists a returned loan
    private Loan saveReturnedLoan(LibraryUser user, Book book) {
        Loan loan = Loan.builder()
                .libraryUser(user)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(20))
                .dueAt(LocalDateTime.now().minusDays(6))
                .returnedAt(LocalDateTime.now().minusDays(5))
                .build();
        return loanRepository.save(loan);
    }

    // Persists an overdue loan (still active but past due date)
    private Loan saveOverdueLoan(LibraryUser user, Book book) {
        Loan loan = Loan.builder()
                .libraryUser(user)
                .book(book)
                .borrowedAt(LocalDateTime.now().minusDays(20))
                .dueAt(LocalDateTime.now().minusDays(2))
                .returnedAt(null)
                .build();
        return loanRepository.save(loan);
    }


    // Test 1 – GET /api/library/account/my-borrowings  →  active borrowings only

    @Test
    void shouldGetActiveBorrowingsOnly() {
        // Given – student has 1 active loan, 1 returned loan, 1 overdue loan
        Book book1 = saveBook("ISBN-001", "Effective Java", "Joshua Bloch");
        Book book2 = saveBook("ISBN-002", "Clean Code", "Robert C. Martin");
        Book book3 = saveBook("ISBN-003", "Refactoring", "Martin Fowler");

        saveActiveLoan(studentUser, book1);
        saveReturnedLoan(studentUser, book2);
        saveOverdueLoan(studentUser, book3);

        // When – authenticated student requests current borrowings
        ResponseEntity<LoanHistoryResponse[]> response = restClient.get()
                .uri("/api/library/account/my-borrowings")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        // Then – response is 200 OK with only the 2 unreturned loans (active + overdue)
        // The returned loan should NOT appear in this list
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody())
                .extracting(LoanHistoryResponse::getIsbn)
                .containsExactlyInAnyOrder("ISBN-001", "ISBN-003")
                .doesNotContain("ISBN-002");
    }


    // Test 2 – GET /api/library/account/my-borrowings  →  returns empty list

    @Test
    void shouldReturnEmptyWhenNoActiveBorrowings() {
        // Given – student has no loans

        // When – authenticated student requests current borrowings
        ResponseEntity<LoanHistoryResponse[]> response = restClient.get()
                .uri("/api/library/account/my-borrowings")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        // Then – response is 200 OK with empty array
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }


    // Test 3 – GET /api/library/account/my-borrowings  →  status mapping

    @Test
    void shouldResolveStatusCorrectlyForActiveBorrowings() {
        // Given – one active loan and one overdue loan
        Book book1 = saveBook("ISBN-001", "Effective Java", "Joshua Bloch");
        Book book2 = saveBook("ISBN-002", "Refactoring", "Martin Fowler");

        saveActiveLoan(studentUser, book1);
        saveOverdueLoan(studentUser, book2);

        // When – authenticated student requests current borrowings
        ResponseEntity<LoanHistoryResponse[]> response = restClient.get()
                .uri("/api/library/account/my-borrowings")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        // Then – statuses are correctly resolved (BORROWED for active, OVERDUE for past-due)
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(2);
        assertThat(response.getBody())
                .extracting(LoanHistoryResponse::getStatus)
                .containsExactlyInAnyOrder(LoanStatus.BORROWED, LoanStatus.OVERDUE);
    }


    // Test 4 – GET /api/library/account/me  →  full borrowing history

    @Test
    void shouldGetFullBorrowingHistory() {
        // Given – student has 1 active loan and 1 returned loan
        Book book1 = saveBook("ISBN-001", "Effective Java", "Joshua Bloch");
        Book book2 = saveBook("ISBN-002", "Clean Code", "Robert C. Martin");

        saveActiveLoan(studentUser, book1);
        saveReturnedLoan(studentUser, book2);

        // When – authenticated student requests full borrowing history
        ResponseEntity<LoanHistoryResponse[]> response = restClient.get()
                .uri("/api/library/account/me")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        // Then – response is 200 OK with BOTH active and returned loans
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(2);
        assertThat(response.getBody())
                .extracting(LoanHistoryResponse::getIsbn)
                .containsExactlyInAnyOrder("ISBN-001", "ISBN-002");

        // Verify the returned loan has status RETURNED and a returnedAt timestamp
        LoanHistoryResponse returned = java.util.Arrays.stream(response.getBody())
                .filter(l -> "ISBN-002".equals(l.getIsbn()))
                .findFirst()
                .orElseThrow();
        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(returned.getReturnedAt()).isNotNull();
    }


    // Test 5 – GET /api/library/account/me  →  empty when student has no loans

    @Test
    void shouldReturnEmptyHistoryWhenNoLoans() {
        // Given – student has no loans

        // When – authenticated student requests full history
        ResponseEntity<LoanHistoryResponse[]> response = restClient.get()
                .uri("/api/library/account/me")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        // Then – response is 200 OK with empty array
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }


    // Test 6 – GET endpoints  →  isolate students (one student doesn't see others)

    @Test
    void shouldNotReturnOtherStudentsLoans() {
        // Given – student2 has 2 loans, student1 has none
        Book book1 = saveBook("ISBN-001", "Effective Java", "Joshua Bloch");
        Book book2 = saveBook("ISBN-002", "Clean Code", "Robert C. Martin");

        saveActiveLoan(otherStudent, book1);
        saveActiveLoan(otherStudent, book2);

        // When – student1 requests their own borrowings
        ResponseEntity<LoanHistoryResponse[]> response = restClient.get()
                .uri("/api/library/account/my-borrowings")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        // Then – student1 sees no loans (student2's loans are hidden)
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();

        // Verify student2 still sees their own loans
        ResponseEntity<LoanHistoryResponse[]> otherResponse = restClient.get()
                .uri("/api/library/account/my-borrowings")
                .header("Authorization", "Bearer " + otherStudentToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        assertThat(otherResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(otherResponse.getBody()).isNotNull().hasSize(2);
    }


    // Test 7 – Endpoints reject ADMIN role (only STUDENT can access)

    @Test
    void shouldRejectAdminFromMyBorrowings() {
        // When – ADMIN tries to access student-only endpoint
        ResponseEntity<String> response = restClient.get()
                .uri("/api/library/account/my-borrowings")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UnauthorizedOperationException from getCurrentStudentUser)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void shouldRejectAdminFromBorrowingHistory() {
        // When – ADMIN tries to access student-only endpoint
        ResponseEntity<String> response = restClient.get()
                .uri("/api/library/account/me")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UnauthorizedOperationException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }


    // Test 8 – Endpoints require authentication (no token → 401)

    @Test
    void shouldReturn401WhenAccessingMyBorrowingsWithoutToken() {
        // When – unauthenticated GET is sent
        ResponseEntity<String> response = restClient.get()
                .uri("/api/library/account/my-borrowings")
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void shouldReturn401WhenAccessingBorrowingHistoryWithoutToken() {
        // When – unauthenticated GET is sent
        ResponseEntity<String> response = restClient.get()
                .uri("/api/library/account/me")
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}