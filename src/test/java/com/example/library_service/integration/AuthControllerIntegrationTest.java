package com.example.library_service.integration;

import com.example.library_service.dto.LibraryAuthResponse;
import com.example.library_service.entity.LibraryRole;
import com.example.library_service.entity.LibraryUser;
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
// Integration test for Library AuthController covering login and PIN change endpoints
class AuthControllerIntegrationTest {


    // Testcontainers: real MySQL container as backend database
    // Using mysql:8.0 to avoid breaking changes introduced in MySQL 9.x

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

    @Autowired
    private JwtService jwtService;


    // Shared test state

    private RestClient restClient;
    private LibraryUser libraryUser;
    private String userToken;


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

        // ---- Persist a STUDENT library user with a known PIN ----
        libraryUser = LibraryUser.builder()
                .studentId("STU-LIB-001")
                .pinHash(passwordEncoder.encode("123456"))
                .role(LibraryRole.STUDENT)
                .firstLogin(true)
                .active(true)
                .build();
        libraryUser = libraryUserRepository.save(libraryUser);

        // Generate a valid JWT for use by /change-pin tests
        userToken = jwtService.generateToken(new CustomLibraryUserDetails(libraryUser));
    }

    @AfterEach
    void tearDown() {
        // Clean up after each test to keep them independent
        libraryUserRepository.deleteAll();
    }


    // Test 1 – POST /api/library/auth/login  →  successful login

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
        // Given – valid credentials matching the seeded user
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-LIB-001");
        request.put("pin", "123456");

        // When – POST /api/library/auth/login is called
        ResponseEntity<LibraryAuthResponse> response = restClient.post()
                .uri("/api/library/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(LibraryAuthResponse.class);

        // Then – response is 200 OK with token and user info
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();
        assertThat(response.getBody().getStudentId()).isEqualTo("STU-LIB-001");
        assertThat(response.getBody().getRole()).isEqualTo(LibraryRole.STUDENT);
        assertThat(response.getBody().isFirstLogin()).isTrue();

        // JWT tokens have three dot-separated parts (header.payload.signature)
        assertThat(response.getBody().getToken().split("\\.")).hasSize(3);
    }


    // Test 2 – POST /api/library/auth/login  →  fails with wrong PIN

    @Test
    void shouldFailLoginWithWrongPin() {
        // Given – valid studentId but wrong PIN
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-LIB-001");
        request.put("pin", "999999");

        // When – POST /api/library/auth/login is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (InvalidCredentialsException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }


    // Test 3 – POST /api/library/auth/login  →  fails when user does not exist

    @Test
    void shouldFailLoginWhenUserDoesNotExist() {
        // Given – non-existent studentId
        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-DOES-NOT-EXIST");
        request.put("pin", "123456");

        // When – POST /api/library/auth/login is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (UserNotFoundException)
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }


    // Test 4 – POST /api/library/auth/login  →  fails when account is inactive

    @Test
    void shouldFailLoginWhenAccountInactive() {
        // Given – mark the user as inactive
        libraryUser.setActive(false);
        libraryUserRepository.save(libraryUser);

        Map<String, Object> request = new HashMap<>();
        request.put("studentId", "STU-LIB-001");
        request.put("pin", "123456");

        // When – POST /api/library/auth/login is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (InvalidCredentialsException: "Library account is inactive")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();
    }


    // Test 5 – POST /api/library/auth/login  →  fails on validation when fields missing

    @Test
    void shouldFailLoginWhenStudentIdMissing() {
        // Given – payload missing studentId
        Map<String, Object> request = new HashMap<>();
        request.put("pin", "123456");

        // When – POST /api/library/auth/login is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Bean Validation rejects with 400 Bad Request
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }


    // Test 6 – POST /api/library/auth/change-pin  →  successfully changes PIN

    @Test
    void shouldChangePinSuccessfully() {
        // Given – authenticated user with current PIN "123456"
        Map<String, Object> request = new HashMap<>();
        request.put("oldPin", "123456");
        request.put("newPin", "654321");

        // When – authenticated POST /api/library/auth/change-pin is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/auth/change-pin")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – response is 200 OK with success message
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("PIN changed successfully");

        // Verify PIN was actually updated and firstLogin flag set to false
        LibraryUser updated = libraryUserRepository.findByStudentId("STU-LIB-001").orElseThrow();
        assertThat(passwordEncoder.matches("654321", updated.getPinHash())).isTrue();
        assertThat(passwordEncoder.matches("123456", updated.getPinHash())).isFalse();
        assertThat(updated.isFirstLogin()).isFalse();
    }


    // Test 7 – POST /api/library/auth/change-pin  →  fails with wrong old PIN

    @Test
    void shouldFailChangePinWhenOldPinIncorrect() {
        // Given – authenticated user but providing wrong old PIN
        Map<String, Object> request = new HashMap<>();
        request.put("oldPin", "wrong-pin");
        request.put("newPin", "654321");

        // When – authenticated POST /api/library/auth/change-pin is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/auth/change-pin")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – server rejects (InvalidCredentialsException: "Old PIN is incorrect")
        assertThat(response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError()).isTrue();

        // Verify PIN was NOT changed
        LibraryUser unchanged = libraryUserRepository.findByStudentId("STU-LIB-001").orElseThrow();
        assertThat(passwordEncoder.matches("123456", unchanged.getPinHash())).isTrue();
    }


    // Test 8 – POST /api/library/auth/change-pin  →  fails on validation when fields missing

    @Test
    void shouldFailChangePinWhenNewPinMissing() {
        // Given – payload missing newPin
        Map<String, Object> request = new HashMap<>();
        request.put("oldPin", "123456");

        // When – authenticated POST /api/library/auth/change-pin is called
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/auth/change-pin")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Bean Validation rejects with 400 Bad Request
        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
    }


    // Test 9 – POST /api/library/auth/change-pin  →  401 without JWT token
    //          (NOTE: this endpoint is NOT in the permitAll list,
    //          so it requires authentication)

    @Test
    void shouldReturn401WhenChangingPinWithoutToken() {
        // Given – payload without Authorization header
        Map<String, Object> request = new HashMap<>();
        request.put("oldPin", "123456");
        request.put("newPin", "654321");

        // When – unauthenticated POST is sent
        ResponseEntity<String> response = restClient.post()
                .uri("/api/library/auth/change-pin")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(String.class);

        // Then – Spring Security rejects with 401 Unauthorized
        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }


    // Test 10 – Full flow: login → use returned token to change PIN → re-login with new PIN

    @Test
    void shouldLoginChangePinAndReLoginWithNewPin() {
        // ---- Step 1: Login with original PIN ----
        Map<String, Object> loginRequest = new HashMap<>();
        loginRequest.put("studentId", "STU-LIB-001");
        loginRequest.put("pin", "123456");

        ResponseEntity<LibraryAuthResponse> loginResponse = restClient.post()
                .uri("/api/library/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(loginRequest)
                .retrieve()
                .toEntity(LibraryAuthResponse.class);

        assertThat(loginResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(loginResponse.getBody()).isNotNull();
        String tokenFromLogin = loginResponse.getBody().getToken();

        // ---- Step 2: Use that token to change PIN ----
        Map<String, Object> changePinRequest = new HashMap<>();
        changePinRequest.put("oldPin", "123456");
        changePinRequest.put("newPin", "999000");

        ResponseEntity<String> changePinResponse = restClient.post()
                .uri("/api/library/auth/change-pin")
                .header("Authorization", "Bearer " + tokenFromLogin)
                .contentType(MediaType.APPLICATION_JSON)
                .body(changePinRequest)
                .retrieve()
                .toEntity(String.class);

        assertThat(changePinResponse.getStatusCode().value()).isEqualTo(200);

        // ---- Step 3: Re-login with the new PIN ----
        Map<String, Object> reLoginRequest = new HashMap<>();
        reLoginRequest.put("studentId", "STU-LIB-001");
        reLoginRequest.put("pin", "999000");

        ResponseEntity<LibraryAuthResponse> reLoginResponse = restClient.post()
                .uri("/api/library/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(reLoginRequest)
                .retrieve()
                .toEntity(LibraryAuthResponse.class);

        assertThat(reLoginResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(reLoginResponse.getBody()).isNotNull();
        // After PIN change, firstLogin flag should be false
        assertThat(reLoginResponse.getBody().isFirstLogin()).isFalse();

        // ---- Step 4: Old PIN should no longer work ----
        Map<String, Object> oldPinLoginRequest = new HashMap<>();
        oldPinLoginRequest.put("studentId", "STU-LIB-001");
        oldPinLoginRequest.put("pin", "123456");

        ResponseEntity<String> oldPinResponse = restClient.post()
                .uri("/api/library/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(oldPinLoginRequest)
                .retrieve()
                .toEntity(String.class);

        assertThat(oldPinResponse.getStatusCode().is4xxClientError()
                || oldPinResponse.getStatusCode().is5xxServerError()).isTrue();
    }
}