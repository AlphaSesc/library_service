package com.example.library_service.integration;

import com.example.library_service.dto.LoanHistoryResponse;
import com.example.library_service.dto.StudentLoanSummaryResponse;
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
// Integration test for AdminLoanController covering admin-only loan monitoring endpoints
class AdminLoanControllerIntegrationTest {


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
    private LibraryUser student1;
    private LibraryUser student2;
    private String adminToken;
    private String studentToken;


    // Setup & Teardown


    @BeforeEach
    void setUp() {
        // Build RestClient pointing at the embedded server
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { /* don't throw on errors */ })
                .build();

        // ---- Persist an ADMIN user ----
        LibraryUser adminUser = LibraryUser.builder()
                .studentId("ADMIN-001")
                .pinHash(passwordEncoder.encode("admin123"))
                .role(LibraryRole.ADMIN)
                .firstLogin(false)
                .active(true)
                .build();
        libraryUserRepository.save(adminUser);
        adminToken = jwtService.generateToken(new CustomLibraryUserDetails(adminUser));

        // ---- Persist two STUDENT users ----
        student1 = LibraryUser.builder()
                .studentId("STU-LIB-001")
                .pinHash(passwordEncoder.encode("123456"))
                .role(LibraryRole.STUDENT)
                .firstLogin(false)
                .active(true)
                .build();
        student1 = libraryUserRepository.save(student1);
        studentToken = jwtService.generateToken(new CustomLibraryUserDetails(student1));

        student2 = LibraryUser.builder()
                .studentId("STU-LIB-002")
                .pinHash(passwordEncoder.encode("123456"))
                .role(LibraryRole.STUDENT)
                .firstLogin(false)
                .active(true)
                .build();
        student2 = libraryUserRepository.save(student2);
    }

    @AfterEach
    void tearDown() {
        // Clear in dependency-safe order: loans → books → users
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        libraryUserRepository.deleteAll();
    }


    // Helpers

    private Book saveBook(String isbn, String title) {
        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .author("Some Author")
                .totalCopies(5)
                .availableCopies(5)
                .active(true)
                .build();
        return bookRepository.save(book);
    }

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


    // Test 1 – GET /api/admin/library/loans/current  →  active loans across system

    @Test
    void shouldGetAllCurrentLoans() {
        // Given – 2 active loans, 1 overdue (still active), 1 returned (excluded)
        Book book1 = saveBook("ISBN-001", "Effective Java");
        Book book2 = saveBook("ISBN-002", "Clean Code");
        Book book3 = saveBook("ISBN-003", "Refactoring");
        Book book4 = saveBook("ISBN-004", "Domain-Driven Design");

        saveActiveLoan(student1, book1);
        saveActiveLoan(student2, book2);
        saveOverdueLoan(student1, book3);
        saveReturnedLoan(student2, book4);

        // When – admin requests current loans
        ResponseEntity<LoanHistoryResponse[]> response = restClient.get()
                .uri("/api/admin/library/loans/current")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        // Then – response is 200 OK with 3 unreturned loans (active + overdue),
        // returned loan is excluded
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody())
                .extracting(LoanHistoryResponse::getIsbn)
                .containsExactlyInAnyOrder("ISBN-001", "ISBN-002", "ISBN-003")
                .doesNotContain("ISBN-004");
    }


    // Test 2 – GET /api/admin/library/loans/current  →  empty when no active loans

    @Test
    void shouldReturnEmptyCurrentLoansWhenNoneExist() {
        // Given – no loans in the system

        // When – admin requests current loans
        ResponseEntity<LoanHistoryResponse[]> response = restClient.get()
                .uri("/api/admin/library/loans/current")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        // Then – response is 200 OK with empty array
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }


    // Test 3 – GET /api/admin/library/loans/overdue  →  overdue loans only

    @Test
    void shouldGetOnlyOverdueLoans() {
        // Given – 1 active, 2 overdue, 1 returned
        Book book1 = saveBook("ISBN-001", "Effective Java");
        Book book2 = saveBook("ISBN-002", "Clean Code");
        Book book3 = saveBook("ISBN-003", "Refactoring");
        Book book4 = saveBook("ISBN-004", "Domain-Driven Design");

        saveActiveLoan(student1, book1);
        saveOverdueLoan(student1, book2);
        saveOverdueLoan(student2, book3);
        saveReturnedLoan(student2, book4);

        // When – admin requests overdue loans
        ResponseEntity<LoanHistoryResponse[]> response = restClient.get()
                .uri("/api/admin/library/loans/overdue")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        // Then – response contains only the 2 overdue loans
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(2);
        assertThat(response.getBody())
                .extracting(LoanHistoryResponse::getIsbn)
                .containsExactlyInAnyOrder("ISBN-002", "ISBN-003");

        // Verify all returned loans have OVERDUE status
        assertThat(response.getBody())
                .extracting(LoanHistoryResponse::getStatus)
                .containsOnly(LoanStatus.OVERDUE);
    }


    // Test 4 – GET /api/admin/library/loans/overdue  →  empty when none overdue

    @Test
    void shouldReturnEmptyOverdueLoansWhenNoneAreOverdue() {
        // Given – only active (not overdue) loans exist
        Book book1 = saveBook("ISBN-001", "Effective Java");
        saveActiveLoan(student1, book1);

        // When – admin requests overdue loans
        ResponseEntity<LoanHistoryResponse[]> response = restClient.get()
                .uri("/api/admin/library/loans/overdue")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(LoanHistoryResponse[].class);

        // Then – response is 200 OK with empty array
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().isEmpty();
    }


    // Test 5 – GET /api/admin/library/students/loan-summary  →  per-student summary

    @Test
    void shouldGetStudentLoanSummaries() {
        // Given:
        //   student1 → 2 active (1 normal + 1 overdue)
        //   student2 → 1 active (overdue) + 1 returned (excluded from counts)
        Book book1 = saveBook("ISBN-001", "Effective Java");
        Book book2 = saveBook("ISBN-002", "Clean Code");
        Book book3 = saveBook("ISBN-003", "Refactoring");
        Book book4 = saveBook("ISBN-004", "Domain-Driven Design");

        saveActiveLoan(student1, book1);     // student1: counts as active
        saveOverdueLoan(student1, book2);    // student1: counts as active + overdue
        saveOverdueLoan(student2, book3);    // student2: counts as active + overdue
        saveReturnedLoan(student2, book4);   // student2: NOT counted (returned)

        // When – admin requests student loan summaries
        ResponseEntity<StudentLoanSummaryResponse[]> response = restClient.get()
                .uri("/api/admin/library/students/loan-summary")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(StudentLoanSummaryResponse[].class);

        // Then – response contains a summary for each STUDENT user
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(2);

        // Find student1's summary
        StudentLoanSummaryResponse summary1 = java.util.Arrays.stream(response.getBody())
                .filter(s -> "STU-LIB-001".equals(s.getStudentId()))
                .findFirst()
                .orElseThrow();
        assertThat(summary1.getBooksOnLoan()).isEqualTo(2);
        assertThat(summary1.getOverdueBooks()).isEqualTo(1);

        // Find student2's summary
        StudentLoanSummaryResponse summary2 = java.util.Arrays.stream(response.getBody())
                .filter(s -> "STU-LIB-002".equals(s.getStudentId()))
                .findFirst()
                .orElseThrow();
        assertThat(summary2.getBooksOnLoan()).isEqualTo(1);
        assertThat(summary2.getOverdueBooks()).isEqualTo(1);

        // Verify ADMIN is NOT in the summary (only STUDENT role)
        assertThat(response.getBody())
                .extracting(StudentLoanSummaryResponse::getStudentId)
                .doesNotContain("ADMIN-001");
    }


    // Test 6 – GET /api/admin/library/students/loan-summary  →  zero counts for students with no loans

    @Test
    void shouldReturnZeroCountsForStudentsWithoutLoans() {
        // Given – 2 students exist, neither has any loans

        // When – admin requests summaries
        ResponseEntity<StudentLoanSummaryResponse[]> response = restClient.get()
                .uri("/api/admin/library/students/loan-summary")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(StudentLoanSummaryResponse[].class);

        // Then – both students appear with 0 counts
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(2);
        assertThat(response.getBody())
                .extracting(StudentLoanSummaryResponse::getBooksOnLoan)
                .containsOnly(0L);
        assertThat(response.getBody())
                .extracting(StudentLoanSummaryResponse::getOverdueBooks)
                .containsOnly(0L);
    }


    // Test 7 – Endpoints reject STUDENT role

    @Test
    void shouldRejectStudentFromGettingCurrentLoans() {
        // When – STUDENT tries to access admin endpoint
        ResponseEntity<String> response = restClient.get()
                .uri("/api/admin/library/loans/current")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (Spring Security blocks /api/admin/** for non-ADMIN)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void shouldRejectStudentFromGettingOverdueLoans() {
        ResponseEntity<String> response = restClient.get()
                .uri("/api/admin/library/loans/overdue")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }

    @Test
    void shouldRejectStudentFromGettingLoanSummaries() {
        ResponseEntity<String> response = restClient.get()
                .uri("/api/admin/library/students/loan-summary")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }


    // Test 8 – Endpoints require authentication (no token → 401)

    @Test
    void shouldReturn401WhenAccessingCurrentLoansWithoutToken() {
        ResponseEntity<String> response = restClient.get()
                .uri("/api/admin/library/loans/current")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void shouldReturn401WhenAccessingOverdueLoansWithoutToken() {
        ResponseEntity<String> response = restClient.get()
                .uri("/api/admin/library/loans/overdue")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void shouldReturn401WhenAccessingLoanSummariesWithoutToken() {
        ResponseEntity<String> response = restClient.get()
                .uri("/api/admin/library/students/loan-summary")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}