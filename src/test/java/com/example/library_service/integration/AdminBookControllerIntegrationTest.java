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
// Integration test for AdminBookController covering admin-only book operations
class AdminBookControllerIntegrationTest {


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

        // ---- Persist an ADMIN library user and generate JWT ----
        LibraryUser adminUser = LibraryUser.builder()
                .studentId("ADMIN-001")
                .pinHash(passwordEncoder.encode("admin123"))
                .role(LibraryRole.ADMIN)
                .firstLogin(false)
                .active(true)
                .build();
        libraryUserRepository.save(adminUser);
        adminToken = jwtService.generateToken(new CustomLibraryUserDetails(adminUser));

        // ---- Persist a STUDENT library user (used to verify role rejection) ----
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

    private Book saveBook(String isbn, String title, String author, int copies) {
        Book book = Book.builder()
                .isbn(isbn)
                .title(title)
                .author(author)
                .totalCopies(copies)
                .availableCopies(copies)
                .active(true)
                .build();
        return bookRepository.save(book);
    }


    // Test 1 – GET /api/admin/library/books  →  admin retrieves all books

    @Test
    void shouldGetAllBooksAsAdmin() {
        // Given – two books in the database
        saveBook("978-0-13-468599-1", "Effective Java", "Joshua Bloch", 5);
        saveBook("978-0-13-235088-4", "Clean Code", "Robert C. Martin", 3);

        // When – authenticated admin sends GET request
        ResponseEntity<BookResponse[]> response = restClient.get()
                .uri("/api/admin/library/books")
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toEntity(BookResponse[].class);

        // Then – response is 200 OK with both books
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull().hasSize(2);
    }


    // Test 2 – POST /api/admin/library/books  →  admin successfully adds book

    @Test
    void shouldAddBookAsAdmin() {
        // Given – book payload
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "978-0-13-468599-1");
        request.put("title", "Effective Java");
        request.put("author", "Joshua Bloch");
        request.put("totalCopies", 5);

        // When – authenticated admin sends POST request
        ResponseEntity<BookResponse> response = restClient.post()
                .uri("/api/admin/library/books")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(BookResponse.class);

        // Then – response is 200 OK with created book details
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getIsbn()).isEqualTo("978-0-13-468599-1");
        assertThat(response.getBody().getTitle()).isEqualTo("Effective Java");
        assertThat(response.getBody().getAuthor()).isEqualTo("Joshua Bloch");
        assertThat(response.getBody().getTotalCopies()).isEqualTo(5);
        assertThat(response.getBody().getAvailableCopies()).isEqualTo(5);
        assertThat(response.getBody().isActive()).isTrue();

        // Verify book is actually persisted in the database
        assertThat(bookRepository.findByIsbn("978-0-13-468599-1")).isPresent();
    }


    // Test 3 – POST /api/admin/library/books  →  fails when ISBN already exists

    @Test
    void shouldFailAddingBookWhenIsbnAlreadyExists() {
        // Given – a book with this ISBN already exists
        saveBook("978-0-13-468599-1", "Existing Book", "Some Author", 2);

        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "978-0-13-468599-1");
        request.put("title", "Effective Java");
        request.put("author", "Joshua Bloch");
        request.put("totalCopies", 5);

        // When – authenticated admin tries to add a book with same ISBN
        ResponseEntity<String> response = restClient.post()
                .uri("/api/admin/library/books")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceAlreadyExistsException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify only one book exists with that ISBN
        assertThat(bookRepository.findAll()).hasSize(1);
    }


    // Test 4 – POST /api/admin/library/books  →  validation fails for missing fields

    @Test
    void shouldFailAddingBookWhenIsbnMissing() {
        // Given – payload missing ISBN
        Map<String, Object> request = new HashMap<>();
        request.put("title", "Effective Java");
        request.put("author", "Joshua Bloch");
        request.put("totalCopies", 5);

        // When – authenticated admin sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/admin/library/books")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Bean Validation rejects with 400 Bad Request
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }


    // Test 5 – POST /api/admin/library/books  →  validation fails for zero copies

    @Test
    void shouldFailAddingBookWhenTotalCopiesIsZero() {
        // Given – payload with 0 copies (violates @Min(1))
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "978-0-13-468599-1");
        request.put("title", "Effective Java");
        request.put("author", "Joshua Bloch");
        request.put("totalCopies", 0);

        // When – authenticated admin sends POST request
        ResponseEntity<String> response = restClient.post()
                .uri("/api/admin/library/books")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Bean Validation rejects with 400 Bad Request
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }


    // Test 6 – POST /api/admin/library/books  →  forbidden for STUDENT role

    @Test
    void shouldRejectStudentFromAddingBook() {
        // Given – book payload
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "978-0-13-468599-1");
        request.put("title", "Effective Java");
        request.put("author", "Joshua Bloch");
        request.put("totalCopies", 5);

        // When – STUDENT user tries to add a book
        ResponseEntity<String> response = restClient.post()
                .uri("/api/admin/library/books")
                .header("Authorization", "Bearer " + studentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security returns 403 Forbidden (admin-only endpoint)
        assertThat(response.getStatusCode().value()).isEqualTo(401);

        // Verify no book was created in the database
        assertThat(bookRepository.findAll()).isEmpty();
    }


    // Test 7 – GET /api/admin/library/books  →  forbidden for STUDENT role

    @Test
    void shouldRejectStudentFromGettingAdminBooksView() {
        // When – STUDENT tries to access admin endpoint
        ResponseEntity<String> response = restClient.get()
                .uri("/api/admin/library/books")
                .header("Authorization", "Bearer " + studentToken)
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security returns 403 Forbidden
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }


    // Test 8 – Endpoints require authentication (no token → 401)

    @Test
    void shouldReturn401WhenNoTokenProvidedOnGet() {
        // When – unauthenticated GET is sent
        ResponseEntity<String> response = restClient.get()
                .uri("/api/admin/library/books")
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void shouldReturn401WhenNoTokenProvidedOnPost() {
        // Given – book payload without Authorization header
        Map<String, Object> request = new HashMap<>();
        request.put("isbn", "978-0-13-468599-1");
        request.put("title", "Effective Java");
        request.put("author", "Joshua Bloch");
        request.put("totalCopies", 5);

        // When – unauthenticated POST is sent
        ResponseEntity<String> response = restClient.post()
                .uri("/api/admin/library/books")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }
}