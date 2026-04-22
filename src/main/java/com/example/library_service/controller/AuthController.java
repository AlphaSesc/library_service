package com.example.library_service.controller;

import com.example.library_service.dto.*;
import com.example.library_service.entity.LibraryUser;
import com.example.library_service.security.CustomLibraryUserDetails;
import com.example.library_service.security.JwtService;
import com.example.library_service.service.LibraryUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/library/auth")
// Controller handling authentication and credential management for library users
public class AuthController {

    private final LibraryUserService libraryUserService;
    private final JwtService jwtService;

    public AuthController(LibraryUserService libraryUserService, JwtService jwtService) {
        this.libraryUserService = libraryUserService;
        this.jwtService = jwtService;
    }

    // Authenticates user using studentId and PIN, then returns JWT token
    @PostMapping("/login")
    public LibraryAuthResponse login(@Valid @RequestBody LibraryLoginRequest request) {
        LibraryUser user = libraryUserService.authenticate(
                request.getStudentId(),
                request.getPin()
        );

        // Generate JWT token for authenticated user
        String token = jwtService.generateToken(new CustomLibraryUserDetails(user));

        return new LibraryAuthResponse(
                token,
                user.getStudentId(),
                user.getRole(),
                user.isFirstLogin()
        );
    }

    // Allows authenticated user to change their PIN
    @PostMapping("/change-pin")
    public ResponseEntity<String> changePin(@Valid @RequestBody ChangePinRequest request) {
        libraryUserService.changePin(request);
        return ResponseEntity.ok("PIN changed successfully");
    }
}