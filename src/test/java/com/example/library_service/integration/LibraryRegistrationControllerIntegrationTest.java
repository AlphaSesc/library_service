package com.example.library_service.integration;

import com.example.library_service.dto.LibraryUserResponse;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.repository.LibraryUserRepository;
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
// Integration test for LibraryRegistrationController covering student registration in the library system.
// This endpoint is publicly accessible (used by Student Portal during enrollment).
class LibraryRegistrationControllerIntegrationTest {


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
    private PasswordEncoder passwordEncoder;


    // Shared test state

    private RestClient restClient;


    // Setup & Teardown


    @BeforeEach
    void setUp() {
        // Build RestClient pointing at the embedded server.
        // Custom status handler prevents RestClient from throwing on 4xx/5xx,
        // so we can assert error statuses directly.
        restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { /* don't throw on errors */ })
                .build();
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test to keep them independent
        libraryUserRepository.deleteAll();
    }


    // Test 1 – POST /api/library/register  →  successfully registers a student

    @Test
    void shouldRegisterStudentSuccessfully() {
        // Given – registration payload with a new studentId
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-NEW-001");

        // When – POST /api/library/register is called (no auth needed)
        ResponseEntity<LibraryUserResponse> response = restClient.post()
                .uri("/api/library/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(LibraryUserResponse.class);

        // Then – response is 200 OK with library user details
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-NEW-001");
        assertThat(response.getBody().getRole()).isEqualTo(LibraryRole.STUDENT);
        assertThat(response.getBody().isFirstLogin()).isTrue();
        assertThat(response.getBody().isActive()).isTrue();

        // Verify user is actually persisted with default PIN "000000"
        LibraryUser saved = libraryUserRepository.findByStudentId("STU-NEW-001").orElseThrow();
        assertThat(saved.getStudentId()).isEqualTo("STU-NEW-001");
        assertThat(saved.getRole()).isEqualTo(LibraryRole.STUDENT);
        assertThat(saved.isFirstLogin()).isTrue();
        assertThat(saved.isActive()).isTrue();
        assertThat(passwordEncoder.matches("000000", saved.getPinHash())).isTrue();
    }


    // Test 2 – POST /api/library/register  →  fails when studentId already exists

    @Test
    void shouldFailRegistrationWhenStudentIdAlreadyExists() {
        // Given – an existing library user with the same studentId
        LibraryUser existing = LibraryUser.builder()
                .studentId("STU-DUPLICATE")
                .pinHash(passwordEncoder.encode("000000"))
                .role(LibraryRole.STUDENT)
                .firstLogin(true)
                .active(true)
                .build();
        libraryUserRepository.save(existing);

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-DUPLICATE");

        // When – POST /api/library/register is called with duplicate studentId
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (ResourceAlreadyExistsException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify only one user exists with that studentId
        assertThat(libraryUserRepository.findAll()).hasSize(1);
    }


    // Test 3 – POST /api/library/register  →  validation fails when studentId blank

    @Test
    void shouldFailRegistrationWhenStudentIdBlank() {
        // Given – payload with empty studentId
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "");

        // When – POST /api/library/register is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Bean Validation rejects with 400 Bad Request
        assertThat(response.getStatusCode().value()).isEqualTo(400);

        // Verify no user was created
        assertThat(libraryUserRepository.findAll()).isEmpty();
    }


    // Test 4 – POST /api/library/register  →  validation fails when studentId missing

    @Test
    void shouldFailRegistrationWhenStudentIdMissing() {
        // Given – payload without studentId field
        Map<String, Object> request = new HashMap<>();

        // When – POST /api/library/register is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Bean Validation rejects with 400 Bad Request
        assertThat(response.getStatusCode().value()).isEqualTo(400);

        // Verify no user was created
        assertThat(libraryUserRepository.findAll()).isEmpty();
    }


    // Test 5 – POST /api/library/register  →  no JWT required (publicly accessible)
    //          (used by Student Portal service during enrollment)

    @Test
    void shouldRegisterWithoutAuthenticationToken() {
        // Given – registration payload (no Authorization header)
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-PUBLIC-001");

        // When – POST /api/library/register is called without any token
        ResponseEntity<LibraryUserResponse> response = restClient.post()
                .uri("/api/library/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(LibraryUserResponse.class);

        // Then – endpoint is publicly accessible per SecurityConfig (permitAll)
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-PUBLIC-001");

        // Verify user was created
        assertThat(libraryUserRepository.findByStudentId("STU-PUBLIC-001")).isPresent();
    }


    // Test 6 – Multiple students can register with different studentIds

    @Test
    void shouldRegisterMultipleStudentsWithDifferentIds() {
        // ---- Register first student ----
        Map<String, Object> request1 = new HashMap<>();
        request1.put("studentId", "STU-001");

        ResponseEntity<LibraryUserResponse> response1 = restClient.post()
                .uri("/api/library/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request1)
                .retrieve()
                .toEntity(LibraryUserResponse.class);

        assertThat(response1.getStatusCode().value()).isEqualTo(200);

        // ---- Register second student ----
        Map<String, Object> request2 = new HashMap<>();
        request2.put("studentId", "STU-002");

        ResponseEntity<LibraryUserResponse> response2 = restClient.post()
                .uri("/api/library/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request2)
                .retrieve()
                .toEntity(LibraryUserResponse.class);

        assertThat(response2.getStatusCode().value()).isEqualTo(200);

        // Verify both users exist with distinct IDs
        assertThat(libraryUserRepository.findAll()).hasSize(2);
        assertThat(libraryUserRepository.findByStudentId("STU-001")).isPresent();
        assertThat(libraryUserRepository.findByStudentId("STU-002")).isPresent();
    }


    // Test 7 – Registered user has correct defaults (PIN, role, flags)

    @Test
    void shouldSetCorrectDefaultsOnRegistration() {
        // Given – registration payload
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-DEFAULTS-001");

        // When – POST /api/library/register is called
        restClient.post()
                .uri("/api/library/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(LibraryUserResponse.class);

        // Then – verify all defaults are set correctly in the database
        LibraryUser saved = libraryUserRepository.findByStudentId("STU-DEFAULTS-001").orElseThrow();

        // Default PIN should be "000000" (encoded)
        assertThat(passwordEncoder.matches("000000", saved.getPinHash())).isTrue();

        // New users should always be STUDENT role (admins are not created via this endpoint)
        assertThat(saved.getRole()).isEqualTo(LibraryRole.STUDENT);

        // First login flag should be true (forces PIN change on first login)
        assertThat(saved.isFirstLogin()).isTrue();

        // Account should be active by default
        assertThat(saved.isActive()).isTrue();

        // Timestamps should be set by @PrePersist
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}